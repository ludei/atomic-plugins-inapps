package com.ludei.inapps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public abstract class AbstractInAppService implements InAppService {

    protected Context mContext;
    protected ArrayList<InAppPurchaseObserver> mObservers = new ArrayList<InAppPurchaseObserver>();
    protected ArrayList<InAppProduct> mProducts;
    protected Map<String, Integer> mStock;
    protected HashMap<String, PurchaseCallback> mPurchaseCallbacks = new HashMap<String, PurchaseCallback>();
    protected ValidationHandler mValidationHandler;


    public AbstractInAppService(Context context)
    {
        this.mContext = context;
        this.loadProductsFromCache();
        this.loadCipheredStock();
    }


    public Context getContext() {
        return mContext;
    }

    @Override
    public void init(InitCompletion callback)
    {
        if (callback != null) {
            callback.onInit(null);
        }
    }

    @Override
    public void addPurchaseObserver(InAppPurchaseObserver observer) {
        if (!mObservers.contains(observer)) {
            mObservers.add(observer);
        }
    }

    @Override
    public void removePurchaseObserver(InAppPurchaseObserver observer) {
        mObservers.remove(observer);
    }

    protected abstract void internalFetchProducts(List<String> productIds, FetchCallback callback);

    @Override
    public void fetchProducts(List<String> productIds, final FetchCallback callback) {
        internalFetchProducts(productIds, new FetchCallback() {
            @Override
            public void onComplete(List<InAppProduct> products, Error error) {
                if (error == null && products != null) {
                    for (InAppProduct product: products) {
                        addProduct(product);
                    }
                    saveProductsToCache();
                }
                if (callback != null) {
                    callback.onComplete(products, error);
                }
            }
        });
    }

    @Override
    public List<InAppProduct> getProducts() {
        return mProducts;
    }

    @Override
    public InAppProduct productForId(String productId) {
        for (InAppProduct product: mProducts) {
            if (product.productId.equals(productId)) {
                return product;
            }
        }
        return null;
    }

    @Override
    public boolean isPurchased(String productId) {
        return stockOfProduct(productId) > 0;
    }

    @Override
    public int stockOfProduct(String productId) {
        return mStock.containsKey(productId) ? mStock.get(productId) : 0;
    }

    @Override
    public boolean canPurchase() {
        return true;
    }

    @Override
    public void purchase(String productId, PurchaseCallback callback) {
        this.purchase(productId, 1, callback);
    }

    @Override
    public void setValidationHandler(ValidationHandler handler) {
        mValidationHandler = handler;
    }

    protected void validate(String receipt, String productId, ValidationCompletion completion) {
        if (mValidationHandler == null) {
            completion.finishValidation(null);
        }
        else {
            mValidationHandler.onValidate(receipt, productId, completion);
        }
    }

    protected void runBackgroundTask(Runnable runnable) {
        Thread test = new Thread(runnable);
        test.start();
    }

    protected void dispatchCallback(Runnable runnable) {
        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(runnable);
        }
        else {
            runnable.run();
        }
    }

    protected void addProduct(InAppProduct product) {
        for (int i = 0; i < mProducts.size(); ++i) {
            if (mProducts.get(i).productId.equals(product.productId)) {
                mProducts.remove(i);
                break;
            }
        }
        mProducts.add(product);
    }

    protected void notifyPurchaseStarted(String productId) {
        for (InAppPurchaseObserver observer: mObservers) {
            observer.onPurchaseStart(this, productId);
        }
    }

    protected void notifyPurchaseFailed(String productId, Error error) {
        for (InAppPurchaseObserver observer: mObservers) {
            observer.onPurchaseFail(this, productId, error);
        }
        PurchaseCallback callback = mPurchaseCallbacks.get(productId);
        if (callback != null) {
            callback.onComplete(null, error);
            mPurchaseCallbacks.remove(callback);
        }
    }

    protected void notifyPurchaseCompleted(InAppPurchase purchase) {
        for (InAppPurchaseObserver observer: mObservers) {
            observer.onPurchaseComplete(this, purchase);
        }
        PurchaseCallback callback = mPurchaseCallbacks.get(purchase.productId);
        if (callback != null) {
            callback.onComplete(purchase, null);
            mPurchaseCallbacks.remove(purchase.productId);
        }
    }

    void saveProductsToCache() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        JSONArray json = new JSONArray();
        for (InAppProduct product: this.mProducts) {
            json.put(product.toJSON());
        }
        editor.putString("inappservice_products",json.toString());
        editor.commit();
    }

    void loadProductsFromCache() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String json = preferences.getString("inappservice_products", "[]");
        mProducts = new ArrayList<InAppProduct>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); ++i) {
                mProducts.add(InAppProduct.fromJSON(array.getJSONObject(i)));
            }

        } catch (JSONException e) {

        }
    }

    /**
     * Return pseudo unique ID
     *
     * @return ID
     */
    public static String getUniquePseudoID()
    {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their phone or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their phone, there will be a duplicate entry
        String serial = null;
        try
        {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        }
        catch (Exception e)
        {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    protected void saveCipheredStock() {

        try {
            JSONObject json = new JSONObject();
            for (String pid: mStock.keySet()) {
                json.put(pid, mStock.get(pid));
            }
            final byte[] bytes = json.toString().getBytes("utf-8");
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(getUniquePseudoID().toCharArray()));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(mContext.getContentResolver(),Settings.Secure.ANDROID_ID).getBytes("utf-8"), 20));
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("inappservice_stock", new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP),"utf-8"));
            editor.commit();

        } catch( Exception e ) {
            e.printStackTrace();
        }

    }

    protected void loadCipheredStock(){
        mStock = new HashMap<String, Integer>();

        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            String value = preferences.getString("inappservice_stock", "");
            if (value.length() == 0) {
                return;
            }
            final byte[] bytes = Base64.decode(value,Base64.DEFAULT);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(getUniquePseudoID().toCharArray()));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID).getBytes("utf-8"), 20));
            String json = new String(pbeCipher.doFinal(bytes),"utf-8");
            JSONObject object = new JSONObject(json);
            Iterator<?> keys = object.keys();
            while( keys.hasNext() ){
                String pid = (String)keys.next();
                this.mStock.put(pid, object.optInt(pid));
            }

        } catch( Exception e) {
            e.printStackTrace();
        }
    }
}
