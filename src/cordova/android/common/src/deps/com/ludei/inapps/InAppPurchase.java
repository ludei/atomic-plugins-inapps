package com.ludei.inapps;


import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Describes an InApp purchase object.
 *
 * @author Imanol Fernández
 * @version 1.0
 */
public class InAppPurchase {

    /**
     * The transaction id.
     */
    public String transactionId;

    /**
     * The product id.
     */
    public String productId;

    /**
     * The date when the purchase was completed.
     */
    public Date purchaseDate;

    /**
     * @return The date when the purchase was completed in unix time
     */
    public long unixTime()
    {
        return purchaseDate != null ? purchaseDate.getTime() : 0;
    }

    /**
     * The quantity of the product purchased.
     */
    public int quantity;

    /**
     * Transforms the purchase information into a JSON object.
     *
     * @return A JSONObject containing the purchase information.
     */
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        try {
            o.putOpt("productId", productId);
            o.putOpt("transactionId", transactionId);
            o.put("purchaseDate", purchaseDate != null ? purchaseDate.getTime() : 0);
            o.put("quantity", quantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o;
    }
}
