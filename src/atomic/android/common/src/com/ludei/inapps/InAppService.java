package com.ludei.inapps;

import android.content.Intent;

import java.util.List;

/**
 * Represents the InApp Service
 *
 * @author Imanol Fernández
 * @version 1.0
 */
public interface InAppService {

    /**
     * Initialize the service and wait for completion
     * Some IAP services like Google Play need to initialize external services
     * @param callback Informs when the service is fully initialized and ready to use
     */
    void init(InitCompletion callback);

    /**
     * Adds a purchase observer.
     *
     * @param observer The purchase observer to add.
     */
    void addPurchaseObserver(InAppPurchaseObserver observer);

    /**
     * Removes a purchase observer.
     *
     * @param observer The purchase observer to remove.
     */
    void removePurchaseObserver(InAppPurchaseObserver observer);

    /**
     * Fetches given the list of products from the remote server.
     *
     * @param productIds A list of product ids.
     * @param callback The fetch callback.
     */
    void fetchProducts(List<String> productIds, FetchCallback callback);

    /**
     * Gets the list of all the local products.
     *
     * @return A list of products.
     */
    List<InAppProduct> getProducts();

    /**
     * Returns the product of the given product id.
     *
     * @param productId
     * @return The product with the given id.
     */
    InAppProduct productForId(String productId);

    /**
     * Returns true if the product has been purchased and false otherwise.
     *
     * @param productId The product id to check.
     * @return A boolean that is true is the product is purchased and false otherwise.
     */
    boolean isPurchased(String productId);

    /**
     * Returns the current stock of a given product.
     *
     * @param productId The product id to check.
     * @return An int representing the stock of the product.
     */
    int stockOfProduct(String productId);

    /**
     * Restores the purchases linked to the user account.
     *
     * @param callback The restore callback.
     */
    void restorePurchases(RestoreCallback callback);

    /**
     * Returns true if the store service is avaialable and false otherwise.
     *
     * @return A booblean that will be true if the store service is available and false otherwise.
     */
    boolean canPurchase();

    /**
     * Purchases the product.
     *
     * @param productId The product id of the product to purchase.
     * @param callback The purchase callback.
     */
    void purchase(String productId, PurchaseCallback callback);

    /**
     * Purchases the product.
     *
     * @param productId The product id of the product to purchase.
     * @param quantity The quantity of the product.
     * @param callback The purchase callback.
     */
    void purchase(String productId, int quantity, PurchaseCallback callback);

    /**
     * Consumes a consumable product already purchased.
     *
     * @param productId The product id of the product to consume.
     * @param quantity The quantity of the product to consume.
     * @param callback The consume callback.
     */
    void consume(String productId, int quantity, ConsumeCallback callback);

    /**
     * Sets the custom validation handler to validate the purchasing process.
     *
     * @param handler The validation handler
     */
    void setValidationHandler(ValidationHandler handler);

    /**
     * Use Ludei's server to validate purchases.
     * To enable validatioon using Ludei's server you first need to create an account in Ludei's Cloud server and create a project with you bundleId.
     */
    void setLudeiServerValidationHandler();

    /**
     * Call it when you receive onActivityResult method in your activity.
     *
     * @param requestCode The request code.
     * @param resultCode The result code.
     * @param data The data of the activity result.
     * @return A boolean containing the result of the activity.
     */
    boolean onActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * Call it when the activity is destroyed.
     */
    void onDestroy();

    /**
     * A class representing an error message.
     */
    public class Error {

        /**
         * The numeric code of the error.
         */
        public int code;

        /**
         * The string message describing the error.
         */
        public String message;

        /**
         * Class constructor specifying a code and a message.
         *
         * @param code The error code.
         * @param message The message describing the error.
         */
        public Error(int code, String message) {
            this.code = code;
            this.message = message;
        }
        public Error(Exception exception) {
            this.message = exception.toString();
        }
    }

    /**
     * The fetch callback returns a list of the fetched products or an error if the process fails.
     */
    public interface FetchCallback {
        void onComplete(List<InAppProduct> products, Error error);
    }

    /**
     * Informs when the restoration process fails.
     */
    public interface RestoreCallback {
        void onComplete(Error error);
    }

    /**
     * Informs when a purchase is completed or when the process fails.
     */
    public interface PurchaseCallback {
        void onComplete(InAppPurchase purchase, Error error);
    }

    /**
     * Informs when a product is consumed properly or when the process fails.
     */
    public interface ConsumeCallback {
        void onComplete(int consumed, Error error);
    }

    /**
     * Informs about the purchasing process.
     * When the purchase starts, when the purchase fails and when the purchase is completed.
     */
    public interface InAppPurchaseObserver {
        void onPurchaseStart(InAppService sender, String productId);
        void onPurchaseFail(InAppService sender, String productId, Error error);
        void onPurchaseComplete(InAppService sender, InAppPurchase purchase);
    }

    /**
     * Provides information about the validation proccess.
     */
    public interface ValidationHandler {
        void onValidate(String receipt, String productId, ValidationCompletion completion);
    }

    /**
     * Informs if the Validation is completed or there was an error in the proccess.
     */
    public interface ValidationCompletion {
        void finishValidation(Error error);
    }

    /**
     * Informs when the service is fully initialized and ready to use
     */
    public interface InitCompletion {
        void onInit(Error error);
    }
}