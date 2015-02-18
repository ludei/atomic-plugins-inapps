package com.ludei.inapps.cordova;

import com.ludei.inapps.googleplay.GooglePlayInAppService;

public class GooglePlayInAppServicePlugin extends InAppServicePlugin {
	
	@Override
	protected void pluginInitialize() {
		service = new GooglePlayInAppService(cordova.getActivity());
    }
}
