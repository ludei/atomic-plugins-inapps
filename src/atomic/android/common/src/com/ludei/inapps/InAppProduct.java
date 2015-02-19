package com.ludei.inapps;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Describes an InApp product.
 *
 * @author Imanol Fernández
 * @version 1.0
 */
public class InAppProduct {
    /**
     * The product id.
     */
    public String productId;

    /**
     * The title of the product.
     */
    public String title;

    /**
     * The description of the product.
     */
    public String description;

    /**
     * The price of the product.
     */
    public double price;

    /**
     * The price of the product in local currency.
     */
    public String localizedPrice;

    /**
     * Transforms a product information into a JSON object.
     *
     * @return A JSONObject containing the product information.
     */
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        try {
            o.putOpt("productId", productId);
            o.putOpt("title", title);
            o.putOpt("description", description);
            o.putOpt("localizedPrice", localizedPrice);
            o.put("price", price);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o;
    }

    /**
     * Gets an InAppProduct object from a JSON object.
     *
     * @param object A JSON object containing the product information.
     * @return An InAppProduct object.
     */
    static InAppProduct fromJSON(JSONObject object) {
        InAppProduct product = new InAppProduct();
        product.productId = object.optString("productId");
        product.title = object.optString("title");
        product.description = object.optString("description");
        product.localizedPrice = object.optString("localizedPrice");
        product.price = object.optDouble("price");
        return product;
    }
 }
