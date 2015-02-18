#include "BaseInAppService.h"
#import "LDInAppService.h"

namespace {
    using namespace ludei::inapps;
    
    inline NSString * toNSString(const string & str) { return [NSString stringWithUTF8String:str.c_str()];}
    
    InAppService::Error toError(NSError * error)
    {
        InAppService::Error result;
        if (error) {
            result.code = error.code;
            result.message = error.localizedDescription.UTF8String;
        }
        return result;
    }
    
    InAppProduct toProduct(LDInAppProduct * product)
    {
        InAppProduct result;
        result.title = product.localizedTitle.UTF8String;
        result.productId = product.productId.UTF8String;
        result.description = product.localizedDescription.UTF8String;
        result.localizedPrice = product.localizedPrice.UTF8String;
        result.price = product.price;
        return result;
    }
    
    InAppPurchase toPurchase(LDInAppPurchase * purchase)
    {
        InAppPurchase result;
        result.productId = purchase.productId.UTF8String;
        result.transactionId = purchase.transactionId.UTF8String;
        result.purchaseDate = purchase.purchaseDate.timeIntervalSince1970;
        result.quantity = purchase.quantity;
        return result;
    }
}

@interface LDInAppObserverBridge : NSObject<LDInAppPurchaseObserver>
@property (nonatomic, assign) BaseInAppService * service;
@end

@implementation LDInAppObserverBridge

-(void) inAppService:(LDInAppService *) service didStartPurchase:(NSString *) productId
{
    _service->notifyPurchaseStarted(productId.UTF8String);
}
-(void) inAppService:(LDInAppService *) service didFailPurchase:(NSString *) productId withError:(NSError *) error
{
    _service->notifyPurchaseFailed(productId.UTF8String, toError(error));
}
-(void) inAppService:(LDInAppService *) service didCompletePurchase:(LDInAppPurchase *) purchase
{
    _service->notifyPurchaseCompleted(toPurchase(purchase));
}

@end

namespace ludei { namespace inapps {
    
    
    class InAppServiceIOS: public BaseInAppService {
    protected:
        LDInAppService * service;
    public:
        InAppServiceIOS(LDInAppService * instance)
        {
            service = instance;
        }
        
        void start() override
        {
            [service start];
        }
        
        void fetchProducts(const std::vector<string> & productIds, const FetchCallback & callback) override
        {
            NSMutableArray * array = [NSMutableArray array];
            for (auto & pid: productIds) {
                [array addObject:toNSString(mapProductId(pid))];
            }
            FetchCallback callbackCopy = callback;
            [service fetchProducts:array completion:^(NSArray *products, NSError *error) {
                std::vector<InAppProduct> result;
                for (LDInAppProduct * p in products) {
                    result.push_back(toProduct(p));
                }
                callbackCopy(result, toError(error));
            }];
        }
        
        std::vector<InAppProduct> getProducts() const override
        {
            std::vector<InAppProduct> result;
            for (LDInAppProduct * p in service.products) {
                result.push_back(toProduct(p));
            }
            return result;
        }
        
        bool productForId(const string & productId, InAppProduct * product) const override
        {
            LDInAppProduct * p = [service productForId: toNSString(mapProductId(productId))];
            if (p) {
                *product = toProduct(p);
                return true;
            }
            return false;
        }
        
        bool isPurchased(const string & productId) const override
        {
            return [service isPurchased:toNSString(mapProductId(productId))];
        }
        
        int32_t stockOfProduct(const string & productId) override
        {
            return (int32_t)[service stockOfProduct:toNSString(mapProductId(productId))];
        }
        
        void restorePurchases(const RestoreCallback & callback) override
        {
            RestoreCallback callbackCopy = callback;
            [service restorePurchases:^(NSError *error) {
                callbackCopy(toError(error));
            }];
        }
        
        bool canPurchase() const override
        {
            return service.canPurchase;
        }
        
        void purchase(const string & productId, int32_t quantity, const PurchaseCallback & callback) override
        {
            PurchaseCallback callbackCopy = callback;
            [service purchase:toNSString(mapProductId(productId)) quantity:quantity completion:^(NSError *error) {
                callbackCopy(InAppPurchase(), toError(error)); //TODO
            }];
        }

        void consume(const string & productId, int32_t quantity, const ConsumeCallback & callback) override
        {
            int32_t consumed = (int32_t)[service consume:toNSString(mapProductId(productId)) quantity:quantity];
            if (callback) {
                callback(consumed, Error());
            }
        }
        
        void finishPurchase(const string & transactionId) override
        {
            [service finishPurchase:toNSString(transactionId)];
        }
        
        void setValidationHandler(const ValidationHandler & handler) override
        {
            if (!handler) {
                [service setValidationHandler:nil];
                return;
            }
            ValidationHandler handlerCopy = handler;
            [service setValidationHandler:^(NSData *validationReceipt, NSString *productId, LDValidationCompletion completion) {
                NSString * receipt = [[NSString alloc] initWithData:validationReceipt encoding:NSUTF8StringEncoding];
                LDValidationCompletion completionCopy = completion;
                handlerCopy(receipt.UTF8String, productId.UTF8String, [=](const Error & error) {
                    
                    NSError * validationResult = nil;
                    if (!error.empty()) {
                        validationResult = [NSError errorWithDomain:@"ValidationError" code:error.code userInfo:@{NSLocalizedDescriptionKey:toNSString(error.message)}];
                    }
                    completionCopy(validationResult);
                });
            }];
            
        }
        
        void setLudeiServerValidationHandler() override
        {
            [service setLudeiServerValidationHandler];
        }
        
    };
    
    InAppService * InAppService::create(const char *className)
    {
        Class c = NSClassFromString([NSString stringWithUTF8String:className]);
        if (c) {
            LDInAppService * service = [[c alloc] init];
            return new InAppServiceIOS(service);
        }
        return nullptr;
    }
    
    InAppService * InAppService::create(InAppProvider provider)
    {
        std::map<InAppProvider, const char *> providers = {
            {InAppProvider::APP_STORE, "LDInAppService"},
        };
        
        if (provider == InAppProvider::AUTO) {
            
            for (auto & it : providers) {
                InAppService * service = InAppService::create(it.second);
                if (service) {
                    return service;
                }
            }
            return nullptr;
        }
        else {
            auto it = providers.find(provider);
            return it != providers.end() ? InAppService::create(it->second)  : nullptr;
        }
    }
    
} }