#pragma once

#include <string>
#include <vector>
#include <map>
#include <functional>

namespace ludei { namespace inapps {
    
    using std::string;
    
    struct InAppProduct {
        string productId;
        string title;
        string description;
        string localizedPrice;
        double price;
    };
    
    struct InAppPurchase {
        string transactionId;
        string productId;
        uint64_t purchaseDate;
        int32_t quantity;
    };
    
    class InAppPurchaseObserver;
    
    enum class InAppProvider {
        AUTO,
        APP_STORE,
        GOOGLE_PLAY,
        AMAZON_APPSTORE,
        
    };
    
    class InAppService {
    public:
        struct Error {
            Error() : code(0) {};
            int32_t code;
            string message;
            inline bool empty() const { return code == 0 && message.empty();}
        };
        typedef std::function<void(const std::vector<InAppProduct> & products, const InAppService::Error & error)> FetchCallback;
        typedef std::function<void(const InAppService::Error & error)> RestoreCallback;
        typedef std::function<void(const InAppPurchase & purchase, const InAppService::Error & error)> PurchaseCallback;
        typedef std::function<void(int32_t consumed, const InAppService::Error & error)> ConsumeCallback;
        typedef std::function<void(const InAppService::Error & error)> ValidationCompletion;
        typedef std::function<void(const string & receipt, const string & productId, const ValidationCompletion & completion)>ValidationHandler;
        
        static InAppService * create(InAppProvider provider = InAppProvider::AUTO);
        static InAppService * create(const char * className);
        virtual ~InAppService() {};
        virtual void addPurchaseObserver(InAppPurchaseObserver * observer) = 0;
        virtual void removePurchaseObserver(InAppPurchaseObserver * observer) = 0;
        virtual void start() = 0;
        virtual void fetchProducts(const std::vector<string> & productIds, const FetchCallback & callback) = 0;
        virtual std::vector<InAppProduct> getProducts() const = 0;
        virtual void mapProductIds(const std::map<string, string> & productsMap) = 0;
        virtual bool productForId(const string & productId, InAppProduct * product) const = 0;
        virtual bool isPurchased(const string & productId) const = 0;
        virtual int32_t stockOfProduct(const string & productId) = 0;
        virtual void restorePurchases(const RestoreCallback & callback) = 0;
        virtual bool canPurchase() const = 0;
        virtual void purchase(const string & productId, int32_t quantity, const PurchaseCallback & callback) = 0;
        inline void purchase(const string & productId, const PurchaseCallback & callback) {
            purchase(productId, 1, callback);
        }
        virtual void consume(const string & productId, int32_t quantity, const ConsumeCallback & callback) = 0;
        virtual void finishPurchase(const string & transactionId) = 0;
        virtual void setValidationHandler(const ValidationHandler & handler) = 0;
        virtual void setLudeiServerValidationHandler() = 0;
    };
    
    class InAppPurchaseObserver {
    public:
        virtual void purchaseStarted(InAppService * service, const string & productId) = 0;
        virtual void purchaseFailed(InAppService * service, const string & productId, const InAppService::Error & error) = 0;
        virtual void purchaseCompleted(InAppService * service, const InAppPurchase & purchase) = 0;
    };
    
} }

