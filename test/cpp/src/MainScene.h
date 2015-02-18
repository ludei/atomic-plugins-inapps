#pragma once
#include "cocos2d.h"
#include "InAppService.h"

using namespace ludei::inapps;


class MainScene : public cocos2d::Layer, public InAppPurchaseObserver
{
public:
    static cocos2d::Scene* createScene();

    virtual bool init();
    
    CREATE_FUNC(MainScene);
protected:
    cocos2d::MenuItem * createButton(const char * text, const cocos2d::ccMenuCallback & callback);
    cocos2d::MenuItem * createSmallButton(const char * text, const cocos2d::ccMenuCallback & callback);
    
    InAppService * service;
    cocos2d::Menu * actionsMenu;
    cocos2d::Label * lblListener;
    cocos2d::Menu * backMenu;
    cocos2d::Menu * productsMenu;
    
    void startLoading();
    void endLoading();
    void showProducts(const std::vector<InAppProduct> & products, bool consumeOnClick);
    void showResult(const string & result);
    void showError(const InAppService::Error & error);
    void consumeProduct(const string & productId);
    void purchaseProduct(const string & productId);
    
#pragma mark InAppPurchaseObserver
    virtual void purchaseStarted(InAppService * service, const string & productId) override;
    virtual void purchaseFailed(InAppService * service, const string & productId, const InAppService::Error & error) override;
    virtual void purchaseCompleted(InAppService * service, const InAppPurchase & purchase) override;
};