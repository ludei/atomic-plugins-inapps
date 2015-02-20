#pragma once

#include <string>
#include <vector>
#include <map>
#include <functional>

namespace ludei { namespace inapps {
    
    using std::string;
    
    /**
     *  Defines an InApp product.
     */
    struct InAppProduct {
        /**
         *  The id of the product.
         */
        string productId;
        /**
         *  The title of the product.
         */
        string title;
        /**
         *  The description of the product.
         */
        string description;
        /**
         *  The price of the product in local currency.
         */
        string localizedPrice;
        /**
         *  The price os the product.
         */
        double price;
    };
    
    /**
     *  Defines an InApp purchase.
     */
    struct InAppPurchase {
        /**
         *  The transaction id.
         */
        string transactionId;
        /**
         *  The id of the product.
         */
        string productId;
        /**
         *  The date when the purchase takes place.
         */
        uint64_t purchaseDate;
        /**
         *  The quantity of the product.
         */
        int32_t quantity;
    };
    
    /**
     *  Defines an InApp purchase observer.
     */
    class InAppPurchaseObserver;
    
    /**
     *  Defines the different InApp provider.
     */
    enum class InAppProvider {
        /**
         *  Automatic. The provider is automatically selected depending on linked classes.
         */
        AUTO,
        /**
         *  App Store.
         */
        APP_STORE,
        /**
         *  Google Play.
         */
        GOOGLE_PLAY,
        /**
         *  Amazon App Store.
         */
        AMAZON_APPSTORE,
        
    };
    
    /**
     *  The InApp service.
     */
    class InAppService {
    public:
        
        /**
         *  Defines an error.
         */
        struct Error {
            Error() : code(0) {};
            /**
             *  The code of the error
             */
            int32_t code;
            /**
             *  The message that describes the error.
             */
            string message;
            /**
             *  Returns true if there is no error.
             *
             *  @return True if the error is empty.
             */
            inline bool empty() const { return code == 0 && message.empty();}
        };
        
        /**
         *  The fetch callback.
         *
         *  @param products The fetched products.
         *  @param error    The error description if the process fails.
         */
        typedef std::function<void(const std::vector<InAppProduct> & products, const InAppService::Error & error)> FetchCallback;
        
        /**
         *  The restore callback.
         *
         *  @param error The error description if the process fails.
         */
        typedef std::function<void(const InAppService::Error & error)> RestoreCallback;
        
        /**
         *  The purchase callback.
         *
         *  @param purchase The purchase information.
         *  @param error    The error description if the process fails.
 
         */
        typedef std::function<void(const InAppPurchase & purchase, const InAppService::Error & error)> PurchaseCallback;
        
        /**
         *  The consume callback.
         *
         *  @param consumed The quantity of product consumed.
         *  @param error    The error description if the process fails.
         */
        typedef std::function<void(int32_t consumed, const InAppService::Error & error)> ConsumeCallback;
        
        
        /**
         *  Defines the validation completion.
         *
         *  @param receipt    The receipt.
         *  @param productId  The id of the product.
         *  @param completion Completion.
         */
        typedef std::function<void(const InAppService::Error & error)> ValidationCompletion;
        
        /**
         *  Defines the validation handler.
         *
         *  @param receipt    The receipt.
         *  @param productId  The id of the product.
         *  @param completion Completion.
         */
        typedef std::function<void(const string & receipt, const string & productId, const ValidationCompletion & completion)>ValidationHandler;
        
        /**
         *  Creates a new InAppService
         *
         *  @param provider The InApp Provider that will be used or AUTO to automatically select the one linked within the binary
         *  @result The InAppService with the selected provider or NULL if the provider is not available
         */
        static InAppService * create(InAppProvider provider = InAppProvider::AUTO);
        
        /**
         *  Creates a new InAppService
         *
         *  @param className The className of the provider
         *  @result The InAppService with the selected provider or NULL if the provider is not available
         */
        static InAppService * create(const char * className);
        
        virtual ~InAppService() {};
        
        /**
         *  Adds an Inapp observer to the process.
         *
         *  @param observer An InApp observer.
         */
        virtual void addPurchaseObserver(InAppPurchaseObserver * observer) = 0;
        
        /**
         *  Removes an Inapp observer from the process.
         *
         *  @param observer An InApp observer.
         */
        virtual void removePurchaseObserver(InAppPurchaseObserver * observer) = 0;
        
        /**
         *  Starts the service.
         *  You should call this method when your InAppPurchaseObservers are already registered.
         */
        virtual void start() = 0;
        
        /**
         *  Requests information about products from the remote Store.
         *  Products are cached in a local DB (@see getProducts)
         *
         *  @param productIds The ids of all the fetched products.
         *  @param callback   The FetchCallback.
         */
        virtual void fetchProducts(const std::vector<string> & productIds, const FetchCallback & callback) = 0;
        
        /**
         *  Returns the cached products.
         *
         *  @return The products.
         */
        virtual std::vector<InAppProduct> getProducts() const = 0;
        
        /**
         *  Asociates an alias to each productId.
         *
         *  @param productsMap The map containing the aliases as keys and the real productId as values.
         */
        virtual void mapProductIds(const std::map<string, string> & productsMap) = 0;
        
        /**
         *  Gets a product given its id.
         *
         *  @param productId The id of the product.
         *  @param product   The product returned by reference
         *
         *  @return True if the products exits.
         */
        virtual bool productForId(const string & productId, InAppProduct * product) const = 0;
        
        /**
         *  Check if a product has been previously purchased.
         *
         *  @param productId the id of the product.
         *
         *  @return True if the product has been purchased and false otherswise.
         */
        virtual bool isPurchased(const string & productId) const = 0;
        
        /**
         *  Returns the stock of a product given its id.
         *
         *  @param productId The id of the product to check.
         *
         *  @return The stock of the given product.
         */
        virtual int32_t stockOfProduct(const string & productId) = 0;
        
        /**
         *  Restores already completed transactions and purchases.
         *
         *  @param callback The RestoreCallback.
         */
        virtual void restorePurchases(const RestoreCallback & callback) = 0;
        
        /**
         *  Resturs YES if the service is available
         *
         *  @return True if the service is available and False otherwise.
         */
        virtual bool canPurchase() const = 0;
        
        /**
        *  Purchases a quantity of a specific product.
         *
         *  @param productId The id of the product.
         *  @param quantity  The quantity.
         *  @param callback  The PurchaseCallback
         */
        virtual void purchase(const string & productId, int32_t quantity, const PurchaseCallback & callback) = 0;
        
        /**
         *  Purchases a product.
         *
         *  @param productId The id of the product.
         *  @param callback  The PurchaseCallback.
         */
        inline void purchase(const string & productId, const PurchaseCallback & callback) {
            purchase(productId, 1, callback);
        }
        
        /**
         *  Consumes a consumable item.
         *
         *  @param productId The id of the product.
         *  @param quantity  The quantity.
         *  @param callback  The ConsumeCallback.
         */
        virtual void consume(const string & productId, int32_t quantity, const ConsumeCallback & callback) = 0;
        
        /**
         *  Removes a finished purchase transaction from the queue.
         *
         *  @param transactionId Transaction id.
         */
        virtual void finishPurchase(const string & transactionId) = 0;
        
        /**
         *  Sets a custom purchase validation handler.
         *  Purchases are always validated to TRUE by default.
         *  Set a custom validation handler to use you own custom server to validate purchases.
         *
         *  @param handler The validation handler.
         */
        virtual void setValidationHandler(const ValidationHandler & handler) = 0;
        
        /**
         *  Use Ludei's server to validate purchases.
         */
        virtual void setLudeiServerValidationHandler() = 0;
    };
    
    /**
     *  Allows to listen to different events regarding the purchasing process.
     */
    class InAppPurchaseObserver {
    public:
        
        /**
         *  Triggered when the purchasing process has started.
         *
         *  @param service   The service.
         *  @param productId The id of the product.
         */
        virtual void purchaseStarted(InAppService * service, const string & productId) = 0;
        
        /**
         *  Triggered when the purchasing process has faild.
         *
         *  @param service   The service.
         *  @param productId The id of the product.
         *  @param error     The reported error description.
         */
        virtual void purchaseFailed(InAppService * service, const string & productId, const InAppService::Error & error) = 0;
        
        /**
         *  Triggered when the purchasing proccess is completed successfully.
         *
         *  @param service  The service.
         *  @param purchase The purchase information.
         */
        virtual void purchaseCompleted(InAppService * service, const InAppPurchase & purchase) = 0;
    };
    
} }

