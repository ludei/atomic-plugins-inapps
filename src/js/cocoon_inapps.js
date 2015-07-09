(function(){

    if (!window.Cocoon && window.cordova && typeof require !== 'undefined') {
        cordova.require('cocoon-plugin-common.Cocoon');
    }
    var Cocoon = window.Cocoon;

    /**
    * @fileOverview
    <h2>About Atomic Plugins</h2>
    <p>Atomic Plugins provide an elegant and minimalist API and are designed with portability in mind from the beginning. Framework dependencies are avoided by design so the plugins can run on any platform and can be integrated with any app framework or game engine.
    <br/> <p>You can contribute and help to create more awesome plugins. </p>
    <h2>Atomic Plugins for InApps</h2>
    <p>This <a src="https://github.com/ludei/atomic-plugins-inapps">repository</a> contains an in-app purchase API designed using the Atomic Plugins paradigm. You can integrate IAPs in your app and take advantage of all the features provided: elegant API, local and server-side receipt validation, secure consumable and non-consumable purchase tracking, local products cache, etc. The API is already available in many languagues and we plan to add more in the future.</p>
    <p>Currently there are 3 in-app purchase providers implemented but new ones can be easily added: </p>
    <ul>
    <li>Apple AppStore (iOS/Mac).</li>
    <li>GooglePlay.</li>
    <li>Amazon AppStore.</li> 
    </ul>
    <h3>Setup your project</h3>
    <p>Releases are deployed to Cordova Plugin Registry. 
    You only have to install the desired plugins using Cordova CLI, CocoonJS CLI or Cocoon.io Cloud Server.</p>
    <ul>
    <code>cordova plugin add com.ludei.inapps.ios.appstore;</code><br/>
    <code>cordova plugin add com.ludei.inapps.android.googleplay;</code><br/>
    <code>cordova plugin add con.ludei.inapps.android.amazon;</code><br/>
    </ul>
    <p>The following JavaScript files will be included in your html project by default during installation:</p>
    <ul>
    <li><a href="https://github.com/ludei/atomic-plugins-inapps/blob/master/src/cordova/js/cocoon_inapps.js">cocoon_inapps.js</a></li>
    <li><a href="https://github.com/ludei/cocoon-common/blob/master/src/js/cocoon.js">cocoon.js</a></li>
    </ul>
    <h3>Documentation</h3>
    <p>In this section you will find all the documentation you need for using this plugin in your Cordova project. 
    Select the specific namespace below to open the relevant documentation section:</p>
    <ul>
    <li><a href="http://ludei.github.io/cocoon-common/dist/doc/js/Cocoon.html">Cocoon</a></li>
    <li><a href="Cocoon.InApp.html">InApp</a></li>
    </ul>
    <h3>API Reference</h3>
    <p>For a complete project that tests all the features provided in the API run the following command:</p>
    <ul><code>gulp create-cordova</code></ul>
    <br/><p>We hope you find everything you need to get going here, but if you stumble on any problems with the docs or the plugins, 
    just drop us a line at our forum and we will do our best to help you out.</p>
    <h3>Tools</h3>
    <a href="http://support.ludei.com/hc/communities/public/topics"><img src="img/cocoon-tools-1.png" /></a>
    <a href="https://cocoon.io/doc"><img src="img/cocoon-tools-2.png" /></a>
    <a href="http://cocoon.io/"><img src="img/cocoon-tools-3.png" /></a>
    * @version 1.0
    */

    /**
     * Cocoon.InApp class provides a multiplatform, easy to use and secure in-app purchase API. 
     * Built-in support for local and server-side receipt validation, consumable and non-consumable purchase tracking and local products cache. 
     * Single JavaScript API for multiple IAP providers.
     *
     * @namespace Cocoon.InApp
     * @example
     * // Basic usage, register callbacks first
     * service = Cocoon.InApp;
     * service.on("purchase", {
     *     start: function(productId) {
     *         console.log("purchase started " + productId);
     *     },
     *     error: function(productId, error) {
     *         console.log("purchase failed " + productId + " error: " + JSON.stringify(error));
     *     },
     *     complete: function(purchase) {
     *         console.log("purchase completed " + JSON.stringify(purchase));
     *     }
     * });
     *
     * // Service initialization
     * service.initialize({
     *     autofinish: true
     * }, 
     * function(error){
     *     if(error){
     *         console.log("Error: " + error);
     *     }
     * });
     *
     * // Fetching products from server 
     * service.fetchProducts(productIds, function(products, error){
     *    if(error){
     *        console.log("Error: " + error);
     *    }
     *    else {
     *        var next = [];
     *        for (var i = 0; i < products.length; ++i) {
     *            var product = products[i];
     *            console.log(product);
     *        }
     *    } 
     * });   
     *
     * // Purchasing products
     * service.purchase(product.productId, 3, function(error) { // Optional sugar callback
     *      if(error){
     *           console.log("Error: " + error);
     *      }
     *      else {
     *           console.log("Successfully purchased);    
     *      }
     * });
     */
    Cocoon.define("Cocoon.InApp", function(extension) {
        "use strict";

        extension.serviceName = "InAppService";
        extension.signal = new Cocoon.Signal();
        extension._canPurchase = true;
        extension._products = [];

        var stock = {};

        /**
         * Syncronizes the stock.
         * @memberof Cocoon.InApp
         * @function syncStock
         * @param {object} products An array of products to be syncronized.
         * @private
         */
        function syncStock(products) {
            for (var i = 0; i < products.length; ++i) {
                stock[products[i].productId] = products[i].stock;
                delete products[i].stock;
            }
        }

        /**
         * Adds stock to a product.
         * @memberof Cocoon.InApp
         * @function addStock
         * @param {string} productId The id of the product.
         * @param {number} n The quantity of product.
         * @private
         */
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

        /**
         * Adds a product.
         * @memberof Cocoon.InApp
         * @function addProduct
         * @param {Cocoon.Inapp.Product} product The product to be added.
         * @private
         */
        function addProduct(product) {
            for (var i = 0; i < extension._products.length; ++i) {
                if (extension._products[i].productId === product.productId) {
                    extension._products[i] = product;
                    return;
                }
            }
            extension._products.push(product);

        }

        /**
         * Starts the InApp Service. This will make the system to initialize the InApp callbacks will start to be received after calling this method.
         * Because of this, you should have set your event handler before calling this method, so you won't lose any callback.
         * @memberof Cocoon.InApp
         * @function initialize
         * @param {Cocoon.InApp.Settings} params The initialization params.
         * @param {function} callback The callback function.It receives the following parameters:
         * - Error.
         * @example
         * Cocoon.InApp.initialize({
         *     autofinish: true
         * }, function(error){
         *      if(error){
         *           console.log("Error: " + error);
         *      }
         * });
         */
        extension.initialize = function(params, callback) {

            Cocoon.exec(this.serviceName, "setListener", [], function(data) {

                var event = data[0];
                if (event === "start") {
                    extension.signal.emit("purchase", "start", [data[1]]);
                } else if (event === "complete") {
                    var purchase = data[1];
                    stock[purchase.productId] = data[2];
                    extension.signal.emit("purchase", "complete", [purchase]);
                } else if (event == "error") {
                    var productId = data[1];
                    var error = data[2];
                    extension.signal.emit("purchase", "error", [productId, error]);
                }

            });

            Cocoon.exec(this.serviceName, "initialize", [params], function(data) {
                extension._canPurchase = data.canPurchase;
                extension._products = data.products;
                syncStock(extension._products);
                if (callback) {
                    callback(data.error);
                }
            });
        };

        /**
         * The object that represents a product in the store.
         * @memberof Cocoon.InApp
         * @name Cocoon.InApp.Settings
         * @property {object} Cocoon.InApp.Settings - The object itself
         * @property {boolean} Cocoon.InApp.Settings.autofinish If True, the transactions will finish automatically.
         */
        extension.Settings = {
            autofinish: "autofinish"
        };

        /**
         * This method allows you to check is the  InApp service is available and enabled in this platform.
         * Not all iOS and Android devices will have the InApp service available or enabled.
         * so you should check if it is before calling any other method.
         * @memberof Cocoon.InApp
         * @function canPurchase
         * @returns {boolean} True if the service is available and false otherwise.
         * Cocoon.InApp.canPurchase();
         */
        extension.canPurchase = function() {
            return this._canPurchase;
        };

        /**
         * Fetches the products information from the store.
         * @memberof Cocoon.InApp
         * @function fetchProducts
         * @param {object} productIds Array of ids of products.
         * @param {function} callback The callback function. 
         * - An array of {@link Cocoon.InApp.Product}.
         * - Error. 
         * @example
         * Cocoon.InApp.fetchProducts(["magic.sword", "health.potion"], function(products, error){
         *     if(error){
         *          console.log("Error: " + error);
         *     }
         *     else{
         *          console.log(JSON.stringify(products));
         *     }     
         * });
         */
        extension.fetchProducts = function(productIds, callback) {
            callback = callback || function() {};
            return Cocoon.exec(this.serviceName, "fetchProducts", [productIds], function(products) {
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
         * @returns {object} An array of {@link Cocoon.InApp.Product} that represents all the local products.
         * @example 
         * var products = Cocoon.InApp.getProducts();
         */
        extension.getProducts = function() {
            return this._products;
        };

        /**
         * Gets the product information given a product indetifier.
         * It uses a local cache, so fetchProducts have to be called before if products are not saved from previous execution.
         * @memberof Cocoon.InApp
         * @function productForId
         * @param {string} productId The product id of the product to be checked.
         * @returns {Cocoon.InApp.Product} The product.
         * var product = Cocoon.InApp.productForId(productId);
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
         * @param {string} productId The product id of the product to be checked.
         * @returns {boolean} A boolean that indicates whether the product has been already purchased.
         * @example
         * console.log(Cocoon.InApp.isPurchased(productId));
         */
        extension.isPurchased = function(productId) {
            return this.stockOfProduct(productId) > 0;
        };

        /**
         * Returns the quantity of available items for a specific productId.
         * @memberof Cocoon.InApp
         * @function stockOfProduct
         * @param {string} productId The product id of the product to be checked.
         * @returns {number} A Number that indicates the available quantity of a productId to consume.
         * @example
         * console.log(Cocoon.InApp.stockOfProduct(product.productId));
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
         * Cocoon.InApp.restorePurchases(function(error) {
         *    if (error){
         *       console.log("Error: " + error);
         *    } else {
         *       console.log("Purchases restored");
         *    }
         * });
         */
        extension.restorePurchases = function(callback) {
            callback = callback || function() {};
            Cocoon.exec(this.serviceName, "restorePurchases", [], function() {
                callback();
            }, function(error) {
                callback(error);
            });
        };

        /**
         * Requests a product purchase given its product id.
         * @memberof Cocoon.InApp
         * @function purchase
         * @param {string} productId The id or alias of the product to be purchased.
         * @param {number} quantity The quantity to be purchased, default value 1.
         * @param {function} callback The callback function. It receives the following parameters:
         * - Error.
         * @example 
         * Cocoon.InApp.purchase(product.productId, 1, function(error) {
         *      if(error){
         *           console.log("Error: " + error);
         *      }
         *      else {
         *           console.log("Successfully purchased);    
         *      }
         * });
         */
        extension.purchase = function(productId, quantity, callback) {
            if (typeof quantity !== "number") {
                quantity = 1;
            }
            callback = callback || function() {};
            Cocoon.exec(this.serviceName, "purchase", [productId, quantity], function() {
                callback();
            }, function(error) {
                callback(error);
            });
        };

        /**
         * Consumes a purchase.
         * This makes that product to be purchasable again (on Android).
         * @memberof Cocoon.InApp
         * @function consume
         * @param {string} productId The id or alias of the product to be consumed.
         * @param {number} quantity The quantity to be consumed, default value 1.
         * @param {function} callback The callback function. It receives the following parameters:
         * - Consumed - The quantity consumed.
         * - Error.
         * @example
         * Cocoon.InApp.consume(product.productId, 3, function(consumed, error) {
         *     if(error){
         *          console.log("Error: " + error);
         *     }
         *     else{
         *          console.log("Consumed items: " + consumed);
         *     }       
         * });
         */
        extension.consume = function(productId, quantity, callback) {
            if (typeof quantity !== "number") {
                quantity = 1;
            }
            callback = callback || function() {};
            Cocoon.exec(this.serviceName, "consume", [productId, quantity], function(consumed) {
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
         * @example
         * Cocoon.InApp.finishPurchase(product.TransactionId);
         */
        extension.finishPurchase = function(transactionId) {
            Cocoon.exec(this.serviceName, "finishPurchase", [transactionId]);
        };

        /**
         * Sets a custom function to validate purchases with your own server
         * @memberof Cocoon.InApp
         * @function setValidationHandler
         * @param {function} validationHandler
         * @example
         * Cocoon.InApp.setValidationHandler(function(receipt, productId, completion){
         *      ... //Custom validation code
         *      console.log("Custom validation: " + receipt + " " + productId);
         *      completion(true); //call completion function with true param if validation succeeds
         * });
         */
        extension.setValidationHandler = function(validationHandler) {
            var noValidation = !validationHandler;
            Cocoon.exec(this.serviceName, "setValidationHandler", [noValidation], function(data) {

                var completionId = data[2];
                validationHandler(data[0], data[1], function(validationResult) {
                    Cocoon.exec(extension.serviceName, "validationCompletion", [completionId, !!validationResult]);
                });
            });
        };

        /**
         *
         * Use Ludei's server to validate purchases.
         * To enable validation using Ludei's server you first need to create an account in Ludei's Cloud server and create a project with you bundleId.
         * @memberof Cocoon.InApp
         * @function setLudeiServerValidationHandler
         * @example
         * Cocoon.InApp.setLudeiServerValidationHandler();
         */
        extension.setLudeiServerValidationHandler = function() {
            Cocoon.exec(this.serviceName, "setLudeiServerValidationHandler", []);
        };

        /**
         * The object that represents a product in the store.
         * @memberof Cocoon.InApp
         * @name Cocoon.InApp.Product
         * @property {object} Cocoon.InApp.Product - The object itself
         * @property {string} Cocoon.InApp.Product.productId The id of the product.
         * @property {string} Cocoon.InApp.Product.title The title of the product.
         * @property {string} Cocoon.InApp.Product.description The description of the product.
         * @property {number} Cocoon.InApp.Product.localizedPrice The price of the product in local currency.
         * @property {number} Cocoon.InApp.Product.price The price of the product.
         */
        extension.Product = {
            productId: "productId",
            title: "title",
            description: "description",
            localizedPrice: "localizedPrice",
            price: "price"
        };

        /**
         * The object that represents the information of a purchase.
         * @memberof Cocoon.InApp
         * @name Cocoon.InApp.PurchaseInfo
         * @property {object} Cocoon.InApp.PurchaseInfo - The object itself
         * @property {string} Cocoon.InApp.PurchaseInfo.productId The product id of the purchase.
         * @property {string} Cocoon.InApp.PurchaseInfo.transactionId The transaction id of the purchase.
         * @property {timestamp} Cocoon.InApp.PurchaseInfo.purchaseDate The date when the purchase was completed.
         * @property {number} Cocoon.InApp.PurchaseInfo.quantity The number of products of the productId kind purchased in this transaction.
         */
        extension.PurchaseInfo = {
            productId: "productId",
            transactionId: "transactionId",
            purchaseDate: "purchaseDate",
            quantity: "quantity"
        };

        /**
         * Allows to listen to events about the purchasing process.
         * - The callback 'start' receives a parameter the product id of the product being purchased when the purchase of a product starts.
         * - The callback 'complete' receives as parameter the {@link Cocoon.InApp.PurchaseInfo} object of the product being purchased when the purchase of a product is completed.
         * - The callback 'error' receives a parameters the product id and an error message when the purchase of a product fails.
         * @memberOf Cocoon.InApp
         * @event On purchase
         * @example
         * Cocoon.InApp.on("purchase", {
         *    start: function(productId) {
         *        console.log("purchase started " + productId);
         *    },
         *    error: function(productId, error) {
         *        console.log("purchase failed " + productId + " error: " + JSON.stringify(error));
         *    },
         *    complete: function(purchase) {
         *        console.log("purchase completed " + JSON.stringify(purchase));
         *    }
         * });
         */
        extension.on = extension.signal.expose();

        return extension;
    });
})();
