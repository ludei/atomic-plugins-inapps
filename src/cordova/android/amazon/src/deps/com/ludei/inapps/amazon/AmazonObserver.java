package com.ludei.inapps.amazon;


import android.content.Context;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;

public class AmazonObserver extends BasePurchasingObserver
{
    private AmazonInAppService mService;

    public AmazonObserver(Context context, AmazonInAppService service) {
        super(context);
        mService = service;
    }

    @Override
    public void onSdkAvailable(boolean isSandbox) {
        mService.onSdkAvailable(isSandbox);
    }

    @Override
    public void onGetUserIdResponse(GetUserIdResponse getUserIdResponse) {
        mService.onGetUserIdResponse(getUserIdResponse);
    }

    @Override
    public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
        mService.onPurchaseResponse(purchaseResponse);
    }

    @Override
    public void onItemDataResponse(ItemDataResponse itemDataResponse) {
        mService.onItemDataResponse(itemDataResponse);
    }

    @Override
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
        mService.onPurchaseUpdatesResponse(purchaseUpdatesResponse);
    }
}
