Cocoon.define("Cocoon.InApp" , function(extension){
    "use strict";

    /**
     * This namespace represents the In-app purchases extension API.
     * <div class="alert alert-success">
     *   Here you will find a demo about this namespace: <a href="https://github.com/ludei/cocoonjs-demos/tree/master/InApp-skeleton">InApp-skeleton demo</a>.
     *</div>
     *
     * <div class="alert alert-warning">
     *    <strong>Warning!</strong> This JavaScript extension requires some configuration parameters on the <a href="https://ludei.zendesk.com/hc/en-us">cloud compiler</a>!
     * </div>
     * @namespace Cocoon.InApp
     * @example
     * // Basic usage, register callbacks first
     * Cocoon.InApp.on("purchase",{
	* 	started: function(){ ... },
	* 	success: function(purchaseInfo){ console.log( JSON.stringify(purchaseInfo) ) },
	* 	error: function(productId, err){ ... }
	* });
     * Cocoon.InApp.on("load",{
	* 	started: function(){ ... },
	*	success: function(products){
    *		for (var i = 0; i < products.length; i++) {
    *			Cocoon.InApp.addProduct(products[i]);
    *			console.log("Adding product to the local database: " + JSON.stringify(products[i]));
    *		};
    *	},
	* 	error: function(errorMessage){ ... }
	* });
     * // Initialize InApp service
     * Cocoon.InApp.initialize({
	*     sandbox: false,
	*     managed: true
	* });
     * // Fetch the products from the InApp
     * // The callbacks for this event are set in the Cocoon.InApp.on("load"); event handler.
     * Cocoon.InApp.loadProducts(["magic.sword", "health.potion"]);
     */

    extension.nativeAvailable = !!Cocoon.nativeAvailable;
    extension.serviceName = "InAppService";
    extension.signal = new Cocoon.Signal();
    extension._canPurchase = true;
    extension._products = [];
    var stock = {};

    function syncStock(products) {
        for (var i = 0; i < products.length; ++i) {
            stock[products[i].productId] = products[i].stock;
            delete products[i].stock;
        }
    }

    function addStock(productId, n) {
        var value = stock[productId];
        if (typeof value !== "number") {
            value = 0;
        }
        value += n;
        if (value < 0) {
            value = 0;
        }
        stock[productId] = value;
    }

    function addProduct(product) {
        for (var i = 0; i <  extension._products.length; ++i) {
            if (extension._products[i].productId === product.productId) {
                extension._products[i] = product;
                return;
            }
        }
        extension._products.push(product);

    }

    /**
     * Starts the InApp Service. This will make the system to initialize the
     * InApp callbacks will start to be received
     * after calling this method. Because of this, you should have set your event handler
     * before calling this method, so you don't lose any callback.
     * @memberof Cocoon.InApp
     * @function initialize
     * @example
     * Cocoon.InApp.initialize();
     */
    extension.initialize = function(params, callback) {

        Cocoon.callNative(this.serviceName, "setListener", [], function(data) {

            var event = data[0];
            if (event === "start") {
                extension.signal.emit("purchase", "start", [data[1]]);
            }
            else if (event === "complete") {
                var purchase = data[1];
                stock[purchase.productId] = data[2];
                extension.signal.emit("purchase", "complete", [purchase]);
            }
            else if (event == "error") {
                var productId = data[1];
                var error = data[2];
                extension.signal.emit("purchase", "error", [productId, error]);
            }

        });

        Cocoon.callNative(this.serviceName, "initialize", [params], function(data){
            extension._canPurchase = data.canPurchase;
            extension._products = data.products;
            syncStock(extension._products);
            if (callback) {
                callback();
            }
        });
    };


    /**
     * This method allows you to check is the  InApp service is available and enabled in this platform.
     * Not all iOS and Android devices will have the InApp service available or enabled
     * so you should check if it is before calling any other method.
     * @memberof Cocoon.InApp
     * @function canPurchase
     * @returns {boolean} True if the service is available and false otherwise.
     */
    extension.canPurchase = function() {
        return this._canPurchase;
    };

    /**
     * Fetches the products information from the store.
     * @memberof Cocoon.InApp
     * @function fetchProducts
     * @example
     * Cocoon.InApp.fetchProducts(["magic.sword", "health.potion"], function(products, error){
     *     console.log(JSON.stringify(products), JSON.stringify(error));
     * });
     */
    extension.fetchProducts = function(productIds, callback) {
        callback = callback || function(){};
        return Cocoon.callNative(this.serviceName, "fetchProducts", [productIds], function(products) {
            for (var i = 0; i < products.length; ++i) {
                addProduct(products[i]);
            }
            syncStock(products);
            callback(products, null);

        }, function(error) {
            callback([], error);
        });
    };

    /**
     * Returns all the locally cached InApp products.
     * @memberof Cocoon.InApp
     * @function getProducts
     * @returns {products} An array with all the local products
     */
    extension.getProducts = function() {
        return this._products;
    };

    /**
     * Get's product info for product indetifier
     * It uses a local cache, so fetchProducts have to be called before if products are not saved from previous execution
     * @memberof Cocoon.InApp
     * @function productForId
     * @returns {products} An array with all the local products
     */
    extension.productForId = function(productId) {
        for (var i = 0; i < this._products.length; ++i) {
            var product = this._products[i];
            if (product.productId === productId) {
                return product;
            }
        }
        return null;
    };

    /**
     * Returns if a product has been already purchased or not.
     * @memberof Cocoon.InApp
     * @function isPurchased
     * @param {string} productId The product id or alias of the product to be checked.
     * @returns {boolean} A boolean that indicates whether the product has been already purchased.
     */
    extension.isPurchased = function(productId) {
        return this.stockOfProduct(productId) > 0;
    };

    /**
     * Returns the quantity of available items for a specific productId
     * @memberof Cocoon.InApp
     * @function stockOfProduct
     * @param {string} productId The product id or alias of the product to be checked.
     * @returns {number} A Number that indicates the available quantity of a productId to consume
     */
    extension.stockOfProduct = function(productId) {
        var quantity = stock[productId];
        return typeof quantity === "number" ? quantity : 0;
    };

    /**
     * Restores all the purchases from the platform's market.
     * For each already purchased product the event "purchase" will be called.
     * @memberof Cocoon.InApp
     * @function restorePurchases
     * @param {function} callback The callback function. It receives the following parameters:
     * - Error.
     * @example
     * Cocoon.Store.restore(function(error) {
     * });
     */
    extension.restorePurchases = function(callback) {
        callback = callback || function(){};
        Cocoon.callNative(this.serviceName, "restorePurchases", [], function() {
            callback();
        }, function(error) {
            callback(error);
        });
    };

    /**
     * Requests a product purchase given it's product id.
     * @memberof Cocoon.InApp
     * @function purchase
     * @param {string} productId The id or alias of the product to be purchased.
     * @param {number} productId The quantity to be purchased, default value 1.
     * @param {function} callback The callback function. It receives the following parameters:
     * - Error.
     * @example
     * Cocoon.Store.on("purchase",{
     * 	start: function(productId){ ... },
     * 	success: function(purchase){ ... },
     * 	error: function(productId, err){ ... }
     * });
     * Cocoon.Store.purchase("magic.sword");
     */
    extension.purchase = function(productId, quantity, callback) {
        if (typeof quantity !== "number") {
            quantity = 1;
        }
        callback = callback || function(){};
        Cocoon.callNative(this.serviceName, "purchase", [productId, quantity], function() {
            callback();
        }, function(error) {
            callback(error);
        });
    };

    /**
     * Consumes a purchase
     * This makes that product to be purchasable again (on Android)
     * @memberof Cocoon.InApp
     * @function consume
     * @param {string} productId The id or alias of the product to be consumed.
     * @param {number} productId The quantity to be consumed, default value 1.
     * @param {function} callback The callback function. It receives the following parameters:
     * - Consumed {number} The quantity consumed
     * - Error.
     */
    extension.consume = function(productId, quantity, callback) {
        if (typeof quantity !== "number") {
            quantity = 1;
        }
        callback = callback || function(){};
        Cocoon.callNative(this.serviceName, "consume", [productId, quantity], function(consumed) {
            addStock(productId, -consumed);
            callback(consumed, null);
        }, function(error) {
            callback(0, error);
        });
    };

    /**
     * Finishes a purchase transaction and removes the transaction from the transaction queue.
     * You don't need to finish purchases if the autoFinishPurchases param is enabled in initialization (enabled by default)
     * This method must be called after a purchase finishes successfully and the "success"
     * event inside of the "on purchase products" callback has been received.
     * If the purchase includes some asset to download from an external server this method must be called after the asset has been successfully downloaded.
     * If you do not finish the transaction because the asset has not been correctly downloaded the "purchase" event will be called again later on.
     * @memberof Cocoon.InApp
     * @function finishPurchase
     * @param {string} transactionId The transactionId of the purchase to finish.
     */
    extension.finishPurchase = function(transactionId) {
        Cocoon.callNative(this.serviceName, "finishPurchase", [transactionId]);
    };

    /**
     * Sets a custom function to validate purchases with your own server
     * @param {function} validationHandler
     * @example
     * Cocoon.InApp.setValidationHandler(function(receipt, productId, completion) {
     *    (...) //Custom validation code
     *    completion(true); //call completion function with true param if validation succeeds
     * });
     */
    extension.setValidationHandler = function(validationHandler) {
        var noValidation = !validationHandler;
        Cocoon.callNative(this.serviceName, "setValidationHandler", [noValidation], function(data){

            var completionId = data[2];
            validationHandler(data[0], data[1], function(validationResult) {
                Cocoon.callNative(extension.serviceName, "validationCompletion", [completionId, !!validationResult]);
            });
        });
    };

    /**
     * Used Ludei's Server to validate purchases
     */
    extension.setLudeiServerValidationHandler = function() {
        Cocoon.callNative(this.serviceName, "setLudeiServerValidationHandler", []);
    };

    extension.on = extension.signal.expose();

    return extension;
});