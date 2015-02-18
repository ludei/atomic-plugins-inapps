#pragma once
#include "BaseInAppService.h"
#include "safejni.h"
using namespace safejni;


namespace ludei { namespace inapps {
    
    /*
     * Bridge between C++ API and atomic android implementation
     */
    
    class InAppServiceAndroid: public BaseInAppService {

    public:
        InAppServiceAndroid(SPJNIObject androidService);
        ~InAppServiceAndroid();

        virtual void start() override;
        virtual void fetchProducts(const std::vector<string> & productIds, const FetchCallback & callback) override;
        virtual std::vector<InAppProduct> getProducts() const override;
        virtual bool productForId(const string & productId, InAppProduct * product) const override;
        virtual bool isPurchased(const string & productId) const override;
        virtual int32_t stockOfProduct(const string & productId) override;
        virtual void restorePurchases(const RestoreCallback & callback) override;
        virtual bool canPurchase() const override;
        virtual void purchase(const string & productId, int32_t quantity, const PurchaseCallback & callback) override;
        virtual void consume(const string & productId, int32_t quantity, const ConsumeCallback & callback) override;
        virtual void finishPurchase(const string & transactionId) override;
        virtual void setValidationHandler(const ValidationHandler & handler) override;
        virtual void setLudeiServerValidationHandler() override;

        SPJNIObject javaObject;
    };


} }