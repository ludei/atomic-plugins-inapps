package com.ludei.inapps.googleplay;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.ludei.inapps.*;
import com.android.vending.billing.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GooglePlayInAppService extends AbstractInAppService
{
    public static class GPInAppPurchase extends InAppPurchase {
        public String signature;
        public String developerPayload;
        public String purchaseToken;
        public int purchaseState;
        public String purchaseData;

        public static GPInAppPurchase from(String purchaseData, String signature) throws JSONException {

            JSONObject jo = new JSONObject(purchaseData);
            GPInAppPurchase purchase = new GPInAppPurchase();
            purchase.signature = signature;
            purchase.purchaseData = purchaseData;
            purchase.productId = jo.getString("productId");
            purchase.transactionId = jo.getString("orderId");
            purchase.quantity = 1;
            purchase.purchaseState = jo.getInt("purchaseState");
            purchase.purchaseToken = jo.getString("purchaseToken");
            purchase.developerPayload = jo.optString("developerPayload");
            purchase.purchaseDate = new Date(jo.optLong("purchaseTime"));
            return purchase;
        }
    }

    private IInAppBillingService mService;
    private InitCompletion mServiceOnConnected;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            if (mServiceOnConnected != null) {
                mServiceOnConnected.onInit(new Error(0, "Service Disconnected"));
                mServiceOnConnected = null;
            }
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            if (mServiceOnConnected != null) {
                mServiceOnConnected.onInit(null);
                mServiceOnConnected = null;
            }
        }
    };
    private String mPendingIntentProductId;
    private String developerPayload = UUID.randomUUID().toString();
    private int apiVersion = 3;


    public GooglePlayInAppService(Context ctx)
    {
        super(ctx);
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void init(InitCompletion callback)
    {
        if (callback == null) {
            return;
        }

        if (mService != null) {
            callback.onInit(null);
        }
        else {
            mServiceOnConnected = callback;
        }
    }

    @Override
    public void onDestroy() {
        if (mService != null) {
            mContext.unbindService(mServiceConn);
        }
    }

    private InAppProduct JSONObjectToInapp(JSONObject object) {
        InAppProduct product = new InAppProduct();

        product.productId = object.optString("productId");
        //String type = object.optString("type");
        product.localizedPrice = object.optString("price");
        product.title = object.optString("title");
        product.description = object.optString("description");
        product.currency = object.optString("price_currency_code");
        String price;
        if (object.has("price_amount_micros")) {
            price = String.valueOf(((float) object.optInt("price_amount_micros")) / 1000000);

        } else {
            String tmpPrice = product.localizedPrice.replace(",", ".");
            price = String.valueOf(tmpPrice.replace(',', '.').substring(0, tmpPrice.length() - 2));
        }
        product.price = new BigDecimal(price).doubleValue();
        return product;
    }

    public void internalFetchProducts(final List<String> productIds, final FetchCallback callback)
    {

        if (mService == null) {
            callback.onComplete(null, new Error(0, "Service disconnected"));
            return;
        }

        runBackgroundTask(new Runnable() {
            @Override
            public void run() {
                final int MAX_SKU = 20; //Google Play limit getSkuDetails

                Error error = null;
                final ArrayList<InAppProduct> products = new ArrayList<InAppProduct>();

                ArrayList<String> pids = new ArrayList<String>(productIds);
                while (pids.size() > 0) {
                    List<String> currentQuery = pids.size() <= MAX_SKU ? pids : pids.subList(0, MAX_SKU);
                    Bundle querySkus = new Bundle();
                    querySkus.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(currentQuery));
                    currentQuery.clear();

                    try {
                        Bundle skuDetails = mService.getSkuDetails(apiVersion, mContext.getPackageName(), "inapp", querySkus);

                        int response = skuDetails.getInt("RESPONSE_CODE");
                        if (response == 0) {
                            ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");

                            for (String productResponse : responseList) {
                                products.add(JSONObjectToInapp(new JSONObject(productResponse)));
                            }
                        } else {
                            error = new Error(response, Utils.getResponseDesc(response));
                        }
                    } catch (Exception ex) {
                        error = new Error(0, ex.toString());
                    }
                }

                final Error finalError = error;
                dispatchCallback(new Runnable() {
                    @Override
                    public void run() {
                        callback.onComplete(products, finalError);
                    }
                });
            }
        });
    }


    @Override
    public boolean canPurchase() {

        try {
            return mService!= null && mService.isBillingSupported(3, mContext.getPackageName(), "inapp") == 0;
        } catch (RemoteException e) {
            return false;
        }
    }

    final private int BUY_INTENT_REQUEST_CODE = 1104389;


    protected void handleAlreadyOwnedError(final String productId) {
        //try to hide the already owned error and simulate a successful purchase
        this.fetchPurchases(productId, 0, new FetchPurchasesCallback() {
            @Override
            public void onCompleted(ArrayList<GPInAppPurchase> purchases, final Error error) {
                 if (error != null || (purchases == null || purchases.size() == 0)) {
                     dispatchCallback(new Runnable() {
                         @Override
                         public void run() {
                            String innerMessage = error != null ? error.message : "Empty purchase array";
                            notifyPurchaseFailed(productId, new Error(0, "Error while trying to restore an already owned item: " + innerMessage));
                         }
                     });
                 }
                else {
                     validatePurchase(purchases.get(0), null);
                }
            }
        });
    }

    @Override
    public void purchase(final String productId, int quantity, final PurchaseCallback callback) {

        if (mService == null) {
            if (callback != null) {
                callback.onComplete(null, new Error(0, "Service disconnected"));
            }
            return;
        }

        if (callback != null) {
            mPurchaseCallbacks.put(productId, callback);
        }
        dispatchCallback(new Runnable() {
            @Override
            public void run() {
                notifyPurchaseStarted(productId);
            }
        });
        try {
            mPendingIntentProductId = productId;
            Bundle buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(), productId, "inapp", developerPayload);
            final int code = buyIntentBundle.getInt("RESPONSE_CODE", 0);

            if (code == Utils.ResponseCode.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                handleAlreadyOwnedError(productId);
                return;
            }
            else if (code != Utils.ResponseCode.BILLING_RESPONSE_RESULT_OK) {
                dispatchCallback(new Runnable() {
                    @Override
                    public void run() {
                        notifyPurchaseFailed(productId, new Error(code, Utils.getResponseDesc(code)));
                    }
                });
                return;
            }
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            ((Activity)mContext).startIntentSenderForResult(pendingIntent.getIntentSender(), BUY_INTENT_REQUEST_CODE, new Intent(), 0, 0, 0);

        }
        catch (final Exception e) {
            dispatchCallback(new Runnable() {
                @Override
                public void run() {
                    notifyPurchaseFailed(productId, new Error(0, e.toString()));
                }
            });
        }

    }

    @Override
    public void consume(final String productId, int quantity, final ConsumeCallback callback)
    {
        if (mService == null) {
            if (callback != null) {
                callback.onComplete(0, new Error(0, "Service disconnected"));
            }
            return;
        }

        fetchPurchases(productId, 0, new FetchPurchasesCallback() {
            @Override
            public void onCompleted(ArrayList<GPInAppPurchase> purchases, final Error error) {

                Error consumeError = error;
                int consumed = 0;
                if (error == null && purchases != null && purchases.size() > 0) {
                    GPInAppPurchase purchase = purchases.get(0);
                    try {
                        int response = mService.consumePurchase(apiVersion, mContext.getPackageName(), purchase.purchaseToken);
                        if (response == 0) {
                            consumed = 1;
                        }
                        else {
                            consumeError = Utils.getResponseError(response);
                        }
                    }
                    catch (Exception e) {
                        consumeError = new Error(e);
                    }

                }
                final Error finalError = consumeError;
                final int finalConsumed = consumed;
                dispatchCallback(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            if (finalConsumed > 0) {
                                mStock.put(productId, 0);
                                saveCipheredStock();
                            }
                            callback.onComplete(finalConsumed, finalError);
                        }
                    }
                });

            }
        });
    }

    protected void validatePurchase(final GPInAppPurchase purchase, final PurchaseCallback callback) {
        //validate developer payload
        if (!this.developerPayload.equals(purchase.developerPayload)) {
            dispatchCallback(new Runnable() {
                @Override
                public void run() {
                    Error error = new Error(0, "Validation failed, developerPayload doesn't match: " + purchase.developerPayload);
                    notifyPurchaseFailed(purchase.productId, error);
                    if (callback != null) {
                        callback.onComplete(purchase, error);
                    }
                }
            });
            return;
        }

        //local validation or server validation
        StringBuilder unverifiedData = new StringBuilder()
                .append("{\"signedData\": " + purchase.purchaseData)
                .append(", ")
                .append("\"signature\": \"" + purchase.signature + "\"}");

        super.validate(unverifiedData.toString(), purchase.productId, new ValidationCompletion() {
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

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode != BUY_INTENT_REQUEST_CODE) {
            return false;
        }

        int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
        String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
        String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
        final String productId = mPendingIntentProductId;
        mPendingIntentProductId = null;

        if (responseCode != Utils.ResponseCode.BILLING_RESPONSE_RESULT_OK) {

            if (responseCode == Utils.ResponseCode.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                handleAlreadyOwnedError(productId);
                return true;
            }
            Error error = new Error(responseCode, Utils.getResponseDesc(responseCode));
            notifyPurchaseFailed(productId, error);
        }
        else if (resultCode != Activity.RESULT_OK) {
            notifyPurchaseFailed(productId, new Error(resultCode, "Activity result not RESULT_OK"));
            return true;
        }
        else {
            try {
                GPInAppPurchase purchase = GPInAppPurchase.from(purchaseData, dataSignature);
                this.validatePurchase(purchase, null);
            } catch (JSONException e) {
                notifyPurchaseFailed(productId, new Error(0, e.toString()));
            }
        }

        return true;
    }

    public interface FetchPurchasesCallback {
        void onCompleted(ArrayList<GPInAppPurchase> purchases, Error error);
    }
    public void fetchPurchases(final String filterProductId, final int filterState, final FetchPurchasesCallback callback) {

        runBackgroundTask(new Runnable() {
            @Override
            public void run() {

                String continuationToken = null;
                try {

                    ArrayList<GPInAppPurchase> purchases = new ArrayList<GPInAppPurchase>();
                    while (true) {
                        Bundle ownedItems = mService.getPurchases(3, mContext.getPackageName(), "inapp", continuationToken);
                        final int response = ownedItems.getInt("RESPONSE_CODE");

                        if (response != 0) {
                            callback.onCompleted(purchases, new Error(response, Utils.getResponseDesc(response)));
                            return;
                        }
                        ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                        continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

                        for (int i = 0; i < purchaseDataList.size(); ++i) {
                            String sku = ownedSkus.get(i);
                            if (filterProductId != null && !filterProductId.equals(sku)) {
                                continue;
                            }
                            String purchaseData = purchaseDataList.get(i);
                            String signature = signatureList.get(i);
                            GPInAppPurchase purchase = GPInAppPurchase.from(purchaseData, signature);
                            purchase.developerPayload = developerPayload;
                            if (filterState <0 || filterState == purchase.purchaseState) {
                                purchases.add(purchase);
                            }

                        }
                        if (continuationToken == null || continuationToken.length() == 0) {
                            callback.onCompleted(purchases, null);
                            break;
                        }
                    }

                }
                catch (final Exception e) {
                    callback.onCompleted(null, new Error(0, e.toString()));
                }
            }
        });
    }

    @Override
    public void restorePurchases(final RestoreCallback callback) {
        if (mService == null) {
            if (callback != null) {
                callback.onComplete(new Error(0, "Service disconnected"));
            }
            return;
        }

        this.fetchPurchases(null, 0, new FetchPurchasesCallback() {
            @Override
            public void onCompleted(ArrayList<GPInAppPurchase> purchases, final Error error) {

                if (error != null) {
                    dispatchCallback(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onComplete(error);
                            }
                        }
                    });
                    return;
                }

                final int[] pending = new int[]{purchases.size()};

                for (final GPInAppPurchase purchase: purchases) {
                    dispatchCallback(new Runnable() {
                        @Override
                        public void run() {
                            notifyPurchaseStarted(purchase.productId);
                        }
                    });
                    validatePurchase(purchase, new PurchaseCallback() {
                        @Override
                        public void onComplete(InAppPurchase purchase, Error error) {

                            pending[0]--;
                            //notify restore completion when all the validations are processed
                            if (pending[0] <=0) {
                                if (callback != null) {
                                    callback.onComplete(null);
                                }

                            }
                        }
                    });
                }

            }
        });

    }

    @Override
    public void setLudeiServerValidationHandler() {
        setValidationHandler(new LudeiServerValidation(this, 1));
    }

}
