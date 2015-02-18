#pragma once

#include "InAppService.h"
#include <algorithm>

namespace ludei { namespace inapps {
    
    class BaseInAppService: public InAppService {
    protected:
        std::vector<InAppPurchaseObserver*> observers;
        std::map<string, string> idsMap;
    public:
        void addPurchaseObserver(InAppPurchaseObserver * observer) override
        {
            if (std::find(observers.begin(), observers.end(), observer) == observers.end()) {
                observers.push_back(observer);
            }
        }
        void removePurchaseObserver(InAppPurchaseObserver * observer) override
        {
            auto it = std::find(observers.begin(), observers.end(), observer);
            if (it != observers.end()) {
                observers.erase(it);
            }
        }
        
        void notifyPurchaseStarted(const string & productId)
        {
            for (InAppPurchaseObserver * observer: observers) {
                observer->purchaseStarted(this, productId);
            }
        }
        
        void notifyPurchaseFailed(const string & productId, const Error & error)
        {
            for (InAppPurchaseObserver * observer: observers) {
                observer->purchaseFailed(this, productId, error);
            }
        }
        
        void notifyPurchaseCompleted(const InAppPurchase & purchase)
        {
            for (InAppPurchaseObserver * observer: observers) {
                observer->purchaseCompleted(this, purchase);
            }
        }
        
        void mapProductIds(const std::map<string, string> & productsMap) override
        {
            this->idsMap = productsMap;
        }
        
        string mapProductId(const string & productId) const {
            auto it = idsMap.find(productId);
            return it != idsMap.end() ? it->second : productId;
        }
        
    };
    
} }

