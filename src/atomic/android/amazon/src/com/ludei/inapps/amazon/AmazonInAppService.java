package com.ludei.inapps.amazon;

import android.content.Context;
import android.content.Intent;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.Receipt;
import com.ludei.inapps.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AmazonInAppService extends AbstractInAppService
{

    private AmazonObserver mAmazonObserver;
    private boolean mSdkAvailable;
    private FetchCallback mFetchCallback;
    private HashMap<String, PurchaseCallback> mPurchaseCallbacks = new HashMap<>();
    private String mAlreadyEntitledRequestId;

    public AmazonInAppService(Context ctx) {
        super(ctx);

        mAmazonObserver = new AmazonObserver(ctx, this);
        PurchasingManager.registerObserver(mAmazonObserver);
    }


    @Override
    protected void internalFetchProducts(List<String> productIds, FetchCallback callback) {
        mFetchCallback = callback;
        Set<String> pIds = new HashSet<>(productIds);
        PurchasingManager.initiateItemDataRequest(pIds);
    }

    @Override
    public boolean canPurchase() {
        return mSdkAvailable;
    }

    @Override
    public void restorePurchases(RestoreCallback callback) {

    }

    @Override
    public void purchase(final String productId, int quantity, PurchaseCallback callback) {

        dispatchCallback(new Runnable() {
            @Override
            public void run() {
                notifyPurchaseStarted(productId);
            }
        });

        String requestId = PurchasingManager.initiatePurchaseRequest(productId);
        mPurchaseCallbacks.put(requestId, callback);
    }

    @Override
    public void consume(String productId, int quantity, ConsumeCallback callback) {

    }

    @Override
    public void setLudeiServerValidationHandler() {
        setValidationHandler(new LudeiServerValidation(this, 1));
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public void onDestroy() {

    }


    protected void validatePurchase(Receipt receipt,String userId, final PurchaseCallback callback) {

        //local validation or server validation
        String unverifiedData =  "{\"userId\": \"" + userId + "\", \"purchaseToken\": \""
                + receipt.getPurchaseToken().replace("\n", "") + "\"}";


        final InAppPurchase purchase = new InAppPurchase();
        purchase.productId = receipt.getSku();
        purchase.transactionId = receipt.getPurchaseToken();
        purchase.quantity = 1;
        purchase.purchaseDate = new Date();

        super.validate(unverifiedData, purchase.productId, new ValidationCompletion() {
            @Override
            public void finishValidation(final Error error) {
                dispatchCallback(new Runnable() {
                    @Override
                    public void run() {
                        if (error != null) {
                            //validation failed
                            notifyPurchaseFailed(purchase.productId, error);
                        }
                        else {
                            //validation completed, update stock and save it.
                            mStock.put(purchase.productId, 1);
                            saveCipheredStock();
                            notifyPurchaseCompleted(purchase);
                        }
                        if (callback != null) {
                            callback.onComplete(purchase, error);
                        }
                    }
                });
            }
        });

    }

    //callbacks from AmazonObserver

    public void onSdkAvailable(boolean isSandbox) {
        mSdkAvailable = true;
    }

    public void onItemDataResponse(ItemDataResponse itemDataResponse) {
        final  ArrayList<InAppProduct> products = new ArrayList<InAppProduct>();
        Error error = null;

        switch (itemDataResponse.getItemDataRequestStatus()) {
            case SUCCESSFUL_WITH_UNAVAILABLE_SKUS:
                // Skus that you cannot purchase will be here.
            case SUCCESSFUL:
                Map<String, Item> items = itemDataResponse.getItemData();
                for (String key : items.keySet()) {
                    Item i = items.get(key);
                    InAppProduct product = new InAppProduct();
                    product.productId = i.getSku();
                    product.title = i.getTitle();
                    product.description = i.getDescription();

                    char firstChar = i.getPrice().charAt(0);
                    boolean firstIsDigit = (firstChar >= '0' && firstChar <= '9');
                    char lastChar = i.getPrice().charAt(i.getPrice().length()-1);
                    boolean lastIsDigit = (lastChar >= '0' && lastChar <= '9');
                    if (firstIsDigit && lastIsDigit) {
                        product.price = Double.parseDouble(i.getPrice());
                    } else if (!firstIsDigit) {
                        product.price = Double.parseDouble(i.getPrice().substring(1));
                    } else if (!lastIsDigit) {
                        product.price = Double.parseDouble(i.getPrice().substring(0, i.getPrice().length()-1));
                    }
                    product.localizedPrice = i.getPrice();
                    products.add(product);
                }

                break;

            case FAILED:
                error = new Error(0, "Products information could not be retrieved");
                break;
        }

        final Error finalError = error;
        dispatchCallback(new Runnable() {
            @Override
            public void run() {
                mFetchCallback.onComplete(products, finalError);
                mFetchCallback = null;
            }
        });

    }

    public void onGetUserIdResponse(GetUserIdResponse getUserIdResponse) {

    }

    public void onPurchaseResponse(PurchaseResponse purchaseResponse)  {
        final Receipt receipt = purchaseResponse.getReceipt();
        final String userId = purchaseResponse.getUserId();
        final String productId = receipt.getSku();
        final PurchaseCallback callback = mPurchaseCallbacks.get(purchaseResponse.getRequestId());
        mPurchaseCallbacks.remove(purchaseResponse.getRequestId());

        switch (purchaseResponse.getPurchaseRequestStatus()) {
            case SUCCESSFUL:
                validatePurchase(receipt, userId, callback);
                break;
            case ALREADY_ENTITLED:
                /*
                 * If the customer has already been entitled to the item, a receipt is not returned.
                 * Fulfillment is done unconditionally, we determine which item should be fulfilled by matching the
                 * request id returned from the initial request with the request id stored in the response.
                 */
                mAlreadyEntitledRequestId = PurchasingManager.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
                break;
            case FAILED:
                /*
                 * If the purchase failed for some reason, (The customer canceled the order, or some other
                 * extraneous circumstance happens) the application ignores the request and logs the failure.
                 */
                dispatchCallback(new Runnable() {
                    @Override
                    public void run() {
                        Error error = new Error(1, "onPurchaseResponse FAILED status");
                        notifyPurchaseFailed(productId, error);
                        if (callback != null) {
                            callback.onComplete(null, error);
                        }
                    }
                });
                break;

            case INVALID_SKU:
                /*
                 * If the sku that was purchased was invalid, the application ignores the request and logs the failure.
                 * This can happen when there is a sku mismatch between what is sent from the application and what
                 * currently exists on the dev portal.
                 */
                dispatchCallback(new Runnable() {
                    @Override
                    public void run() {
                        Error error = new Error(2, "Product purchase error: Item " + productId + " unavailable");
                        notifyPurchaseFailed(productId, error);
                        if (callback != null) {
                            callback.onComplete(null, error);
                        }
                    }
                });
                break;
        }

    }

    /**
     * Is invoked once the call from initiatePurchaseUpdatesRequest is completed.
     * On a successful response, a response object is passed which contains the request id, request status, a set of
     * previously purchased receipts, a set of revoked skus, and the next offset if applicable. If a user downloads your
     * application to another device, this call is used to sync up this device with all the user's purchases.
     *
     * @param purchaseUpdatesResponse
     *            Response object containing the user's recent purchases.
     */
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {


    }


}
