package com.ludei.inapps.cordova;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;

import com.ludei.inapps.InAppProduct;
import com.ludei.inapps.InAppPurchase;
import com.ludei.inapps.InAppService;
import com.ludei.inapps.InAppService.ConsumeCallback;
import com.ludei.inapps.InAppService.Error;
import com.ludei.inapps.InAppService.FetchCallback;
import com.ludei.inapps.InAppService.InAppPurchaseObserver;
import com.ludei.inapps.InAppService.PurchaseCallback;
import com.ludei.inapps.InAppService.RestoreCallback;
import com.ludei.inapps.InAppService.ValidationCompletion;
import com.ludei.inapps.InAppService.ValidationHandler;

public class InAppServicePlugin extends CordovaPlugin implements InAppService.InAppPurchaseObserver {
	
	
	protected InAppService service;
	protected CallbackContext listenerCtx;
	protected int validationIndex = 0;
	protected HashMap<Integer, ValidationCompletion> validationCompletions = new HashMap<Integer, ValidationCompletion>();
	
	protected void pluginInitialize() {
		throw new RuntimeException("Override this method and create the InAppService instance");
    }
	
	
	@Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
		
		try 
		{
			Method method = InAppServicePlugin.class.getDeclaredMethod(action, CordovaArgs.class, CallbackContext.class);
			method.invoke(this, args, callbackContext);
			return true;			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
        return false;
    }
	
	//Life cycle methods
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (service != null) {
			service.onActivityResult(requestCode, resultCode, intent);
		}
    }
	
	@Override
	public void onDestroy() {
		if (service != null) {
			service.onDestroy();
		}
	}
	
	//Bridge methods
	public void setListener(CordovaArgs args, CallbackContext ctx) {
		this.listenerCtx = ctx;
	}
	
	public void initialize(CordovaArgs args, final CallbackContext ctx) throws JSONException {
		service.addPurchaseObserver(this);

		service.init(new InAppService.InitCompletion() {
			@Override
			public void onInit(Error error) {
				JSONObject data = new JSONObject();
				try {
					data.put("products", InAppServicePlugin.this.productsToJSON(service.getProducts()));
					data.put("canPurchase", service.canPurchase());
					if (error != null) {
						data.put("error", errorToJSON(error));
					}
				}
				catch (JSONException e) {
					e.printStackTrace();
				}

				ctx.sendPluginResult(new PluginResult(Status.OK, data));
			}

		});


	}
	
	public void fetchProducts(CordovaArgs args, final CallbackContext ctx) {

		JSONArray array = args.optJSONArray(0);
		if (array == null) {
			ctx.sendPluginResult(new PluginResult(Status.INVALID_ACTION, "Invalid argument"));
			return;
		}
		ArrayList<String> productIds = new ArrayList<String>();
		for (int i = 0; i < array.length(); ++i) {
			productIds.add(array.optString(i, "empty"));
		}
		service.fetchProducts(productIds, new InAppService.FetchCallback() {
			
			@Override
			public void onComplete(final List<InAppProduct> products, Error error) {
				if (error != null) {
					ctx.sendPluginResult(new PluginResult(Status.ERROR, errorToJSON(error)));
				}
				else {
					ctx.sendPluginResult(new PluginResult(Status.OK, productsToJSON(products)));
				}
			}
		});
	}
	
	public void getProducts(CordovaArgs args, CallbackContext ctx) {
		ctx.sendPluginResult(new PluginResult(Status.OK, productsToJSON(service.getProducts())));
	}
	
	public void productforId(CordovaArgs args, CallbackContext ctx) {
		String productId = args.optString(0);
		InAppProduct product = null;
		if (productId != null) {
			product = service.productForId(productId);
		}
		if (product!= null) {
			ctx.sendPluginResult(new PluginResult(Status.OK, product.toJSON()));	
		}
		else {
			ctx.sendPluginResult(new PluginResult(Status.OK));
		}
	}
	
	public void isPurchased(CordovaArgs args, CallbackContext ctx) {
		String productId = args.optString(0);
		boolean purchased = productId != null ? service.isPurchased(productId) : false;
		ctx.sendPluginResult(new PluginResult(Status.OK, purchased));
	}
	
	public void stockOfProduct(CordovaArgs args, CallbackContext ctx) {
		String productId = args.optString(0);
		int stock = productId != null ? service.stockOfProduct(productId) : 0;
		ctx.sendPluginResult(new PluginResult(Status.OK, stock));
	}
	
	public void canPurchase(CordovaArgs args, CallbackContext ctx) {
		ctx.sendPluginResult(new PluginResult(Status.OK, service.canPurchase()));
	}
	
	public void finishPurchase(CordovaArgs args, CallbackContext ctx) {
		//this methods exists for multiplatform JavaScript Compatibility
		ctx.sendPluginResult(new PluginResult(Status.OK, service.canPurchase()));
	}
	
	public void restorePurchases(CordovaArgs args, final CallbackContext ctx) {
		service.restorePurchases(new InAppService.RestoreCallback() {
			@Override
			public void onComplete(Error error) {
				if (error != null) {
					ctx.sendPluginResult(new PluginResult(Status.ERROR, errorToJSON(error)));
				}
				else {
					ctx.sendPluginResult(new PluginResult(Status.OK));
				}
			}
		});
	}
	
	public void purchase(CordovaArgs args, final CallbackContext ctx) {

        cordova.setActivityResultCallback(this);

		String productId = args.optString(0);
		if (productId == null) {
			ctx.sendPluginResult(new PluginResult(Status.ERROR, "Invalid argument"));
			return;
		}
		int quantity = args.optInt(1);
		if (quantity < 1) {
			quantity = 1;
		}
		service.purchase(productId, quantity, new InAppService.PurchaseCallback() {
			@Override
			public void onComplete(InAppPurchase purchase, Error error) {
				if (error != null) {
					ctx.sendPluginResult(new PluginResult(Status.ERROR, errorToJSON(error)));
				}
				else {
					ctx.sendPluginResult(new PluginResult(Status.OK, purchase.toJSON()));
				}
			}
		});			
	}
	
	public void consume(CordovaArgs args, final CallbackContext ctx) {
		String productId = args.optString(0);
		if (productId == null) {
			ctx.sendPluginResult(new PluginResult(Status.ERROR, "Invalid argument"));
			return;
		}
		int quantity = args.optInt(1);
		if (quantity < 1) {
			quantity = 1;
		}
		service.consume(productId, quantity, new InAppService.ConsumeCallback() {
			@Override
			public void onComplete(int consumed, Error error) {
				if (error != null) {
					ctx.sendPluginResult(new PluginResult(Status.ERROR, errorToJSON(error)));
				}
				else {
					ctx.sendPluginResult(new PluginResult(Status.OK, consumed));
				}
			}
		});			
	}
	
	public void setValidationHandler(CordovaArgs args, final CallbackContext ctx) {
		boolean noValidation = args.optBoolean(0);
		if (noValidation) {
			service.setValidationHandler(null);
			return;
		}
		service.setValidationHandler(new InAppService.ValidationHandler() {
			
			@Override
			public void onValidate(String receipt, String productId, ValidationCompletion completion) {
				
				int completionId = validationIndex++;
				validationCompletions.put(completionId, completion);
				JSONArray array = new JSONArray();
				array.put(receipt);
				array.put(productId);
				array.put(completionId);
				PluginResult result = new PluginResult(Status.OK, array);		
				result.setKeepCallback(true);
				ctx.sendPluginResult(result);
			}
		});
	}
	
	public void validationCompletion(CordovaArgs args, CallbackContext ctx) {
		int completionId = args.optInt(0);
		boolean validationResult = args.optBoolean(1);
		InAppService.ValidationCompletion completion = validationCompletions.get(completionId);
		if (completion != null) {
			Error error = null;
			if (!validationResult) {
				error = new Error(0, "Custom validation rejected purchase");
			}
			completion.finishValidation(error);
			validationCompletions.remove(completionId);
		}
	}
	
	public void setLudeiServerValidationHandler(CordovaArgs args, CallbackContext ctx) {
		service.setLudeiServerValidationHandler();
	}
	
	//InAppService.InAppPurchaseObserver
	
	@Override
    public void onPurchaseStart(InAppService sender, String productId)  {
		if (listenerCtx != null) {
			JSONArray data = new JSONArray();
			data.put("start");
			data.put(productId);
			PluginResult result = new PluginResult(Status.OK, data);
			result.setKeepCallback(true);
			listenerCtx.sendPluginResult(result);
		}
    }
    
	@Override
	public void onPurchaseFail(InAppService sender, String productId, Error error) {
		if (listenerCtx != null) {
			JSONArray data = new JSONArray();
			data.put("error");
			data.put(productId);
			data.put(this.errorToJSON(error));
			PluginResult result = new PluginResult(Status.OK, data);
			result.setKeepCallback(true);
			listenerCtx.sendPluginResult(result);
		}
    }
    
	@Override
	public void onPurchaseComplete(InAppService sender, InAppPurchase purchase){
		if (listenerCtx != null) {
			JSONArray data = new JSONArray();
			data.put("complete");
			data.put(purchase.toJSON());
			PluginResult result = new PluginResult(Status.OK, data);
			result.setKeepCallback(true);
			listenerCtx.sendPluginResult(result);
		}
    }
	
	//Utility methods
	
	protected JSONArray productsToJSON(List<InAppProduct> products) {
		JSONArray result = new JSONArray();
		for (InAppProduct p: products) {
			JSONObject obj = p.toJSON();
			try {
				obj.put("stock", service.stockOfProduct(p.productId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			result.put(obj);
		}
		return result;
	}
	
	protected JSONObject errorToJSON(Error error) {
		JSONObject result = new JSONObject();
		try {
			result.put("code", error.code);
			result.put("message", error.message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

};