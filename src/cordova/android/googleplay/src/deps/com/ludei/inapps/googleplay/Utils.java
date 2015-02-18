package com.ludei.inapps.googleplay;

import com.ludei.inapps.InAppPurchase;
import com.ludei.inapps.InAppService;

public class Utils
{

    public class ResponseCode {
        // Billing response codes
        public static final int BILLING_RESPONSE_RESULT_OK = 0;
        public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
        public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
        public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
        public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
        public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
        public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
        public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;
    }

    public static String getResponseDesc(int code) {
        String response = "";
        switch (code) {
            case ResponseCode.BILLING_RESPONSE_RESULT_OK:
                response = "Success";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_USER_CANCELED:
                response = "User pressed back or canceled a dialog";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE:
                response = "Billing API version is not supported for the type requested";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE:
                response = "Requested product is not available for purchase";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR:
                response = "Invalid arguments provided to the API. "
                        + "This error can also indicate that the application was not correctly signed or properly set up for In-app Billing in Google Play, "
                        + "or does not have the necessary permissions in its manifest";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_ERROR:
                response = "Fatal error during the API action";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
                response = "Failure to purchase since item is already owned. Restore your purchases to unlock the item.";
                break;

            case ResponseCode.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED:
                response = "Failure to consume since item is not owned";
                break;

            default:
                response = "Unknown error";
                break;
        }

        return response;
    }
    public static InAppService.Error getResponseError(int code) {
        return new InAppService.Error(code, getResponseDesc(code));
    }

}
