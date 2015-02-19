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
         *  Automatic.
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
        
        static InAppService * create(InAppProvider provider = InAppProvider::AUTO);
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
         */
        virtual void start() = 0;
        
        /**
         *  Fetches products from remote.
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
         *  @param productsMap     The map containing the aliases as keys and the real productId as values.
         */
        virtual void mapProductIds(const std::map<string, string> & productsMap) = 0;
        
        /**
         *  Returns a product given its id.
         *
         *  @param productId The id of the product.
         *  @param product   The product.
         *
         *  @return The product.
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
         *  Restores the purchases.
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
         *  Purchases a product.
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
         *  Finishes a purchase.
         *
         *  @param transactionId Tracsaction id.
         */
        virtual void finishPurchase(const string & transactionId) = 0;
        
        /**
         *  Sets a validation handler.
         *
         *  @param handler The validation handler.
         */
        virtual void setValidationHandler(const ValidationHandler & handler) = 0;
        
        /**
         *  Sets Ludei's validation handler.
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

