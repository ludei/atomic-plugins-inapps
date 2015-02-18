package com.ludei.inapptest;

import android.content.Context;
import com.ludei.inapps.InAppService;
import com.ludei.inapps.amazon.AmazonInAppService;





public class InAppServiceCreator {

    public static InAppService create(Context ctx) {
        return new AmazonInAppService(ctx);
    }
}
