package com.ludei.inapps.cpp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.ludei.inapps.*;
import com.safejni.SafeJNI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InAppServiceBridge implements InAppService.InAppPurchaseObserver, SafeJNI.ActivityLifeCycleListener {


    private InAppService _service;
    private long pointer = 0;
    private Activity _activity;

    public InAppServiceBridge() {

    }

    public boolean init(String adServiceClassName) {

        try {
            Class<?> adServiceClass = Class.forName(adServiceClassName);
            _activity = SafeJNI.INSTANCE.getActivity();
            _service = (InAppService)adServiceClass.getConstructor(Context.class).newInstance(_activity);
            SafeJNI.INSTANCE.addLifeCycleListener(this);
        }
        catch (Exception ex) {
            return false;
        }

        return true;
    }

    public void setPointer(long pointer) {
        this.pointer = pointer;
    }

    protected void runOnThread(Runnable runnable) {
        _activity.runOnUiThread(runnable);
    }

    protected void dispatchNative(Runnable runnable) {
        SafeJNI.INSTANCE.dispatchToNative(runnable);
    }

    public void nativeDestructor() {
        runOnThread(new Runnable() {
            @Override
            public void run() {
                onDestroy();
            }
        });
    }

    public void fetchProducts(String[] productIds, final long callback) {
        _service.fetchProducts(Arrays.asList(productIds), new InAppService.FetchCallback() {
            @Override
            public void onComplete(final List<InAppProduct> products, final InAppService.Error error) {

                dispatchNative(new Runnable() {
                    @Override
                    public void run() {
                        int code = error != null ? error.code : 0;
                        String message = error != null ? error.message : "";

                        nativeFetchCallback(callback, products != null ? products.toArray() : null, code, message);
                    }
                });


            }
        });
    }

    public Object[] getProducts() {
        return _service.getProducts().toArray();
    }

    public Object productForId(String productId) {
        return _service.productForId(productId);
    }

    public boolean isPurchased(String productId) {
        return _service.isPurchased(productId);
    }

    public int stockOfProduct(String productId) {
        return _service.stockOfProduct(productId);
    }

    public boolean canPurchase() {
        return _service.canPurchase();
    }

    public void setLudeiServerValidationHandler() {
        _service.setLudeiServerValidationHandler();
    }

    public void restorePurchases(final long callback) {
        _service.restorePurchases(new InAppService.RestoreCallback() {
            @Override
            public void onComplete(final InAppService.Error error) {
                dispatchNative(new Runnable() {
                    @Override
                    public void run() {
                        int code = error != null ? error.code : 0;
                        String message = error != null ? error.message: "";
                        nativeRestoreCallback(callback, code, message);
                    }
                });
            }
        });
    }

    public void purchase(String productId, int quantity, final long callback) {
        _service.purchase(productId, quantity, new InAppService.PurchaseCallback() {
            @Override
            public void onComplete(final InAppPurchase purchase, final InAppService.Error error) {
                dispatchNative(new Runnable() {
                    @Override
                    public void run() {
                        int code = error != null ? error.code : 0;
                        String message = error != null ? error.message : "";
                        nativePurchaseCallback(callback, purchase, code, message);
                    }
                });
            }
        });
    }

    public void consume(String productId, int quantity, final long callback) {
        _service.consume(productId, quantity, new InAppService.ConsumeCallback() {
            @Override
            public void onComplete(final int consumed, final InAppService.Error error) {
                dispatchNative(new Runnable() {
                    @Override
                    public void run() {
                        int code = error != null ? error.code : 0;
                        String message = error != null ? error.message : "";
                        nativeConsumeCallback(callback, consumed, code, message);
                    }
                });
            }
        });
    }

    //Purchase Observer
    private static native void nativePurchaseStarted(long pointer, String productId);
    private static native void nativePurchaseFailed(long pointer, String productId, int errorCode, String message);
    private static native void nativePurchaseCompleted(long pointer, String transactionId, String productId, int quantity, long date);

    //Async callbacks
    private static native void nativeFetchCallback(long callback, Object[] products, int code, String message);
    private static native void nativeRestoreCallback(long callback, int code, String message);
    private static native void nativePurchaseCallback(long callback, InAppPurchase purchase, int code, String message);
    private static native void nativeConsumeCallback(long callback, int consumed, int code, String message);


    //Purchase Observer
    @Override
    public void onPurchaseStart(InAppService sender, final String productId)
    {
        if (pointer == 0) return;

        dispatchNative(new Runnable() {
            @Override
            public void run() {
                nativePurchaseStarted(pointer, productId);
            }
        });
    }

    @Override
    public void onPurchaseFail(InAppService sender, final String productId, final InAppService.Error error)
    {
        if (pointer == 0) return;
        dispatchNative(new Runnable() {
            @Override
            public void run() {
                int code = error != null ? error.code : 0;
                String message = error != null ? error.message: null;
                nativePurchaseFailed(pointer, productId, code, message);
            }
        });
    }

    @Override
    public void onPurchaseComplete(InAppService sender, final InAppPurchase purchase)
    {
        if (pointer == 0) return;
        dispatchNative(new Runnable() {
            @Override
            public void run() {
                nativePurchaseCompleted(pointer, purchase.transactionId, purchase.productId, purchase.quantity, purchase.purchaseDate.getTime());
            }
        });

    }


    //ActivityLifeCycleListener
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        _service.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {
        SafeJNI.INSTANCE.removeLifeCycleListener(this);
        _service.onDestroy();
    }

}
