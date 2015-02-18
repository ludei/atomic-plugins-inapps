#include "MainScene.h"

#include <string>
#include <sstream>

template <typename T>
std::string to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}


USING_NS_CC;

Scene* MainScene::createScene()
{
    auto scene = Scene::create();
    auto layer = MainScene::create();
    scene->addChild(layer);
    return scene;
}

// on "init" you need to initialize your instance
bool MainScene::init()
{
    if ( !Layer::init() )
    {
        return false;
    }
    
    service = InAppService::create();
    service->start();
    
    Size size = Director::getInstance()->getVisibleSize();

    auto background = Sprite::create("background.jpg");

    double scale =  MAX(size.height/background->getContentSize().height, size.width/background->getContentSize().width);
    background->setScale(scale);
    background->setPosition(Vec2(size.width * 0.5, size.height * 0.5));
    
    this->addChild(background, 0);
    
    
    MenuItem * btnFetch = createButton("Fetch products from store", [=](Ref* sender){
        
        std::vector<string> productIds = {
                              "com.ludei.basketgunner.adremoval",
                              "com.ludei.basketgunner.getpackx1",
                              "com.ludei.basketgunner.getpackx2",
                              "com.ludei.basketgunner.getpackx5",
                              "com.ludei.basketgunner.getpackx20",
                              "com.ludei.basketgunner.getpackx50",
        };
        this->startLoading();
        service->fetchProducts(productIds, [=](const std::vector<InAppProduct> & products, const InAppService::Error & error){
            this->endLoading();
            if (error.empty()) {
                this->showProducts(products, false);
            }
            else {
                this->showError(error);
            }
        });

    });
    
    MenuItem * btnPurchased = createButton("Purchase product", [=](Ref* sender){
        
        auto products = service->getProducts();
        if (products.size() == 0) {
            this->showResult("No cached products. Please, fetch the products from Store");
            return;
        }
        this->showProducts(service->getProducts(), false);
    });
    
    MenuItem * btnConsume = createButton("Consume product", [=](Ref* sender){
        auto products = service->getProducts();
        if (products.size() == 0) {
            this->showResult("No cached products. Please, fetch the products from Store");
            return;
        }
        this->showProducts(service->getProducts(), true);
    });
    
    MenuItem * btnRestore = createButton("Restore purchases", [=](Ref* sender){
        this->startLoading();
        service->restorePurchases([=](const InAppService::Error & error){
            this->endLoading();
            if (error.empty()) {
                this->showResult("Purchases restored");
            }
            else {
                this->showError(error);
            }
        });
    });
    
    
    actionsMenu = Menu::create(btnFetch, btnPurchased, btnConsume, btnRestore, NULL);
    actionsMenu->alignItemsVerticallyWithPadding(20);
    actionsMenu->setPosition(Vec2(size.width/2, size.height/2));
    this->addChild(actionsMenu);
    
    lblListener = Label::createWithSystemFont("-", "Arial", 20);
    lblListener->setAnchorPoint(Vec2(1, 0));
    lblListener->setPosition(Vec2(size.width - 5 , 5));
    
    MenuItem * backItem = createSmallButton("Back", [=](Ref *){
        
        backMenu->setVisible(false);
        if (productsMenu) {
            productsMenu->removeFromParent();
            productsMenu = nullptr;
        }
        actionsMenu->setVisible(true);
        
    });
    backItem->setScale(0.8);
    backMenu = Menu::createWithItem(backItem);
    backMenu->setPosition(Vec2(backItem->getContentSize().width * 0.5 * backItem->getScale(),
                               size.height - backItem->getContentSize().height * 0.5 * backItem->getScale()));
    backMenu->setVisible(false);
    this->addChild(backMenu);
    
    productsMenu = nullptr;
    
    return true;
}

MenuItem * MainScene::createButton(const char * text, const cocos2d::ccMenuCallback & callback)
{
    auto item = MenuItemImage::create("bigbutton1.png", "bigbutton2.png", callback);
    auto label = Label::createWithSystemFont(text, "Arial", item->getContentSize().height * 0.33);
    label->setPosition(Vec2(item->getContentSize().width/2, item->getContentSize().height/2));
    item->addChild(label);
    return item;
}

MenuItem * MainScene::createSmallButton(const char * text, const cocos2d::ccMenuCallback & callback)
{
    auto item = MenuItemImage::create("button1.png", "button2.png", callback);
    auto label = Label::createWithSystemFont(text, "Arial", item->getContentSize().height * 0.33);
    label->setPosition(Vec2(item->getContentSize().width/2, item->getContentSize().height/2));
    item->addChild(label);
    return item;
}


void MainScene::startLoading()
{
    Layer * layer = LayerColor::create(Color4B(0, 0, 0, 200));
    auto label = LabelTTF::create("Loading...", "Arial", 30);
    label->setPosition(Vec2(layer->getContentSize().width * 0.5, layer->getContentSize().height * 0.5));
    layer->addChild(label);
    this->addChild(layer);
}

void MainScene::endLoading()
{
    this->removeChild(this->getChildren().back());
}

void MainScene::consumeProduct(const string & productId)
{
    this->startLoading();
    service->consume(productId, 1, [=](int32_t consumed, const InAppService::Error & error){
        this->endLoading();
        if (error.empty()) {
            this->showProducts(service->getProducts(), true); //reload stock
            this->showResult("Consumed " + to_string(consumed) + " units");
        }
        else {
            this->showError(error);
        }
    });
}

void MainScene::purchaseProduct(const string & productId)
{
    this->startLoading();
    service->purchase(productId, [=](const InAppPurchase & purchase, const InAppService::Error & error){
        this->endLoading();
        if (error.empty()) {
            this->showProducts(service->getProducts(), false); //reload stock
            this->showResult("Purchased succesfully. TransactionId: " + purchase.transactionId);
        }
        else {
            this->showError(error);
        }
    });
}

void MainScene::showProducts(const std::vector<InAppProduct> & products, bool consumeOnClick)
{
    actionsMenu->setVisible(false);
    backMenu->setVisible(true);
    
    Vector<MenuItem*> items;
    for (auto & product: products) {
        string text = product.title;
        text += " " + product.localizedPrice;
        text += " stock: " + to_string((service->stockOfProduct(product.productId)));
        
        items.pushBack(createButton(text.c_str(), [=](Ref *){
            
            if (consumeOnClick) {
                consumeProduct(product.productId);
            }
            else {
                purchaseProduct(product.productId);
            }
        }));
    }
    
    if (productsMenu) {
        productsMenu->removeFromParent();
    }
    productsMenu = Menu::createWithArray(items);
    productsMenu->alignItemsVerticallyWithPadding(15);
    productsMenu->setPosition(Vec2(this->getContentSize().width/2, this->getContentSize().height/2));
    this->addChild(productsMenu);
}

void MainScene::showResult(const string & result)
{
    Layer * layer = LayerColor::create(Color4B(0, 0, 0, 200));
    
    auto label = LabelTTF::create(result.c_str(), "Arial", 30);
    label->setPosition(Vec2(layer->getContentSize().width * 0.5, layer->getContentSize().height * 0.5));
    layer->addChild(label);
    
    MenuItem * ok = createButton("Ok", [=](Ref *){
        layer->removeFromParent();
    });
    Menu * menu = Menu::createWithItem(ok);
    menu->setPosition(Vec2(layer->getContentSize().width * 0.5, layer->getContentSize().height * 0.25));
    layer->addChild(menu);

    this->addChild(layer);
}

void MainScene::showError(const InAppService::Error & error)
{
    string message = "Error: " + error.message + " (code: " + to_string(error.code) + ")";
    this->showResult(message);
}

#pragma mark InAppPurchase Observer

void MainScene::purchaseStarted(InAppService * service, const string & productId)
{
    lblListener->setString("purchase started: " + productId);
}

void MainScene::purchaseFailed(InAppService * service, const string & productId, const InAppService::Error & error)
{
    lblListener->setString("purchase failed: " + productId);
}

void MainScene::purchaseCompleted(InAppService * service, const InAppPurchase & purchase)
{
    lblListener->setString("purchase completed: " + purchase.productId);
}

