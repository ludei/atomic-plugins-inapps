package com.ludei.inapps.cordova;

import com.ludei.inapps.amazon.AmazonInAppService;

public class amazonInAppServicePlugin extends InAppServicePlugin {
	
	@Override
	protected void pluginInitialize() {
		service = new AmazonInAppService(cordova.getActivity());
    }
}
