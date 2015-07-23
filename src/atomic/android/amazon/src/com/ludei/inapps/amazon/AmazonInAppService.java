package com.ludei.inapps.amazon;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;

import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserDataResponse;
import com.ludei.inapps.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AmazonInAppService extends AbstractInAppService implements PurchasingListener
{
    private FetchCallback mFetchCallback;
    private HashMap<RequestId, PurchaseCallback> mPurchaseCallbacks = new HashMap<RequestId, PurchaseCallback>();
    private HashMap<RequestId, String> mProductIds = new HashMap<RequestId, String>();
    private RequestId mAlreadyPurchasedRequestId;
    private String mAlreadyPurchasedProductId;

    public AmazonInAppService(Context ctx) {
        super(ctx);

        PurchasingService.registerListener(ctx, this);
    }


    @Override
    protected void internalFetchProducts(List<String> productIds, FetchCallback callback) {
        mFetchCallback = callback;
        Set<String> pIds = new HashSet<String>(productIds);
        PurchasingService.getProductData(pIds);
    }

    @Override
    public boolean canPurchase() {
        return true;
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

        RequestId request = PurchasingService.purchase(productId);
        mProductIds.put(request, productId);
        mPurchaseCallbacks.put(request, callback);
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
                + receipt.getReceiptId().replace("\n", "") + "\"}";


        final InAppPurchase purchase = new InAppPurchase();
        purchase.productId = receipt.getSku();
        purchase.transactionId = receipt.getReceiptId();
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
                        } else {
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

    @Override
    public void onProductDataResponse(ProductDataResponse response) {

        final  ArrayList<InAppProduct> products = new ArrayList<InAppProduct>();
        Error error = null;

        switch (response.getRequestStatus()) {
            case SUCCESSFUL:
                Map<String, Product> items = response.getProductData();
                for (String key : items.keySet()) {
                    Product i = items.get(key);
                    InAppProduct product = new InAppProduct();
                    product.productId = i.getSku();
                    product.title = i.getTitle();
                    product.description = i.getDescription();

                    String localizedPrice = i.getPrice();
                    String price = localizedPrice.replaceAll("[^\\d.]", "");
                    try {
                        product.price = Double.parseDouble(price);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    product.localizedPrice = localizedPrice;
                    products.add(product);
                }

                break;

            case FAILED:
                error = new Error(0, "Products information could not be retrieved");
                break;
            case NOT_SUPPORTED:
                error = new Error(0, "Products information not supported");
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


    @Override
    public void onUserDataResponse(UserDataResponse userDataResponse) {

    }

    @Override
    public void onPurchaseResponse(PurchaseResponse response)  {
        final Receipt receipt = response.getReceipt();
        final String userId = response.getUserData() != null ? response.getUserData().getUserId() : "";
        final String productId = receipt != null ? receipt.getSku() : mProductIds.get(response.getRequestId());
        final PurchaseCallback callback = mPurchaseCallbacks.get(response.getRequestId());
        mPurchaseCallbacks.remove(response.getRequestId());
        mProductIds.remove(response.getRequestId());

        switch (response.getRequestStatus()) {
            case SUCCESSFUL:
                validatePurchase(receipt, userId, callback);
                break;
            case ALREADY_PURCHASED:
                /*
                 * If the customer has already been entitled to the item, a receipt is not returned.
                 * Fulfillment is done unconditionally, we determine which item should be fulfilled by matching the
                 * request id returned from the initial request with the request id stored in the response.
                 */
                mAlreadyPurchasedProductId = productId;
                mAlreadyPurchasedRequestId = PurchasingService.getPurchaseUpdates(true);
                mPurchaseCallbacks.put(mAlreadyPurchasedRequestId, callback);
                break;
            case NOT_SUPPORTED:
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

    @Override
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response) {

        if (response.getRequestId().equals(mAlreadyPurchasedRequestId)) {
            for (Receipt receipt: response.getReceipts()) {
                if (receipt.getSku().equals(mAlreadyPurchasedProductId)) {

                    final PurchaseCallback callback = mPurchaseCallbacks.get(response.getRequestId());
                    if (callback != null) {
                        mPurchaseCallbacks.remove(response.getRequestId());
                    }

                    validatePurchase(receipt, response.getUserData().getUserId(), callback);
                }
            }
            mAlreadyPurchasedProductId = null;
            mAlreadyPurchasedRequestId = null;
        }
    }
}
