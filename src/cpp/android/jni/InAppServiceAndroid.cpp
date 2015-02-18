
#include "InAppServiceAndroid.h"

#define BRIDGE_CLASS_NAME "com/ludei/inapps/cpp/InAppServiceBridge"

#include <jni.h>
#include <android/log.h>

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,"Mortimer",__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"Mortimer",__VA_ARGS__)

using namespace ludei::inapps;

namespace {

    InAppProduct toInAppProduct(jobject obj)
    {
        InAppProduct result;
        if (obj) {
            result.productId = safejni::getField<string>(obj, "productId");
            result.title = safejni::getField<string>(obj, "title");
            result.description = safejni::getField<string>(obj, "description");
            result.localizedPrice = safejni::getField<string>(obj, "localizedPrice");
            result.price = safejni::getField<double>(obj, "price");
        }
        return result;
    }

    InAppPurchase toInAppPurchase(jobject obj)
    {
        InAppPurchase result;
        if (obj) {
            result.productId = safejni::getField<string>(obj, "productId");
            result.transactionId = safejni::getField<string>(obj, "transactionId");
            result.quantity = safejni::getField<int32_t>(obj, "quantity");
            result.purchaseDate = safejni::call<int64_t>(obj, "com/ludei/inapps/InAppPurchase", "unixTime");
        }
        return result;
    }
}

#pragma mark InApp Service Bridge

InAppServiceAndroid::InAppServiceAndroid(SPJNIObject androidService)
{
    javaObject = androidService;
}

InAppServiceAndroid::~InAppServiceAndroid()
{
    javaObject->call("nativeDestructor");
}


void InAppServiceAndroid::start()
{
    //nop op on android
}

void InAppServiceAndroid::fetchProducts(const std::vector<string> & productIds, const FetchCallback & callback)
{
    auto ids = productIds;
    for (int i = 0; i < ids.size(); ++i) {
        ids[i] = mapProductId(productIds[i]);
    }
    javaObject->call("fetchProducts", ids, new FetchCallback(callback));
}

std::vector<InAppProduct> InAppServiceAndroid::getProducts() const
{
    std::vector<jobject> objects = javaObject->call<std::vector<jobject>>("getProducts");
    std::vector<InAppProduct> result;
    for (jobject obj: objects) {
        result.push_back(toInAppProduct(obj));
    }
    return result;
}

bool InAppServiceAndroid::productForId(const string & productId, InAppProduct * product) const
{
    jobject obj = javaObject->call<jobject>("productForId", mapProductId(productId));
    if (obj) {
        *product = toInAppProduct(obj);
        return true;
    }
    return false;
}

bool InAppServiceAndroid::isPurchased(const string & productId) const
{
    return javaObject->call<bool>("isPurchased", mapProductId(productId));
}

int32_t InAppServiceAndroid::stockOfProduct(const string & productId)
{
    return javaObject->call<int>("stockOfProduct", mapProductId(productId));
}

bool InAppServiceAndroid::canPurchase() const
{
    return javaObject->call<bool>("canPurchase");
}

void InAppServiceAndroid::restorePurchases(const RestoreCallback & callback)
{
    javaObject->call("restorePurchases", callback ? new RestoreCallback(callback) : nullptr);
}

void InAppServiceAndroid::purchase(const string & productId, int32_t quantity, const PurchaseCallback & callback)
{
    javaObject->call("purchase", mapProductId(productId), quantity, callback ? new PurchaseCallback(callback) : nullptr);
}

void InAppServiceAndroid::consume(const string & productId, int32_t quantity, const ConsumeCallback & callback)
{
    javaObject->call("consume", mapProductId(productId), quantity, callback ? new ConsumeCallback(callback) : nullptr);
}

void InAppServiceAndroid::finishPurchase(const string & transactionId)
{
    //no op on Android
}

void InAppServiceAndroid::setValidationHandler(const ValidationHandler & handler)
{

}

void InAppServiceAndroid::setLudeiServerValidationHandler()
{
    javaObject->call("setLudeiServerValidationHandler");
}

InAppService * InAppService::create(const char *className)
{
    SPJNIObject service = JNIObject::create(BRIDGE_CLASS_NAME);
    bool initialized = service->call<bool>("init", className);
    if (initialized) {
        InAppServiceAndroid * instance =  new InAppServiceAndroid(service);
        service->call("setPointer", instance);
        return instance;
    }
    return nullptr;
}

InAppService * InAppService::create(InAppProvider provider)
{
        std::map<InAppProvider, const char *> providers = {
            {InAppProvider::GOOGLE_PLAY, "com.ludei.inapps.googleplay.GooglePlayInAppService"},
            {InAppProvider::AMAZON_APPSTORE, "com.ludei.inapps.amazon.AmazonInAppService"}
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


extern "C"
{
    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativePurchaseStarted(JNIEnv * env, jlong pointer, jstring productId)
    {
        InAppServiceAndroid * service = reinterpret_cast<InAppServiceAndroid*>(pointer);
        service->notifyPurchaseStarted(Utils::toString(productId));
    }

    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativePurchaseFailed(JNIEnv * env, jlong pointer, jstring productId, jint code, jstring message)
    {
        InAppServiceAndroid * service = reinterpret_cast<InAppServiceAndroid*>(pointer);
        InAppService::Error error;
        error.code = code;
        error.message = Utils::toString(message);
        service->notifyPurchaseFailed(Utils::toString(productId), error);
    }

    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativePurchaseCompleted(JNIEnv * env, jlong pointer, jstring transactionId, jstring productId, jint quantity, jlong date)
    {
        InAppServiceAndroid * service = reinterpret_cast<InAppServiceAndroid*>(pointer);

        InAppPurchase purchase;
        purchase.transactionId = Utils::toString(transactionId);
        purchase.productId = Utils::toString(productId);
        purchase.quantity = quantity;
        purchase.purchaseDate = date;
        service->notifyPurchaseCompleted(purchase);
    }

    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativeFetchCallback(JNIEnv * env, jlong callback, jobjectArray products, jint code, jstring message)
    {
        InAppService::FetchCallback * fetchCallback = reinterpret_cast<InAppService::FetchCallback *>(callback);
        std::vector<jobject> objects = Utils::toVectorJObject(products);
        std::vector<InAppProduct> result;
        for (jobject obj: objects) {
            result.push_back(toInAppProduct(obj));
        }

        InAppService::Error error;
        error.code = code;
        error.message = Utils::toString(message);

        (*fetchCallback)(result, error);

        delete fetchCallback;
    }

    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativeRestoreCallback(JNIEnv * env, jlong callback, jint code, jstring message)
    {
        InAppService::RestoreCallback * restoreCallback = reinterpret_cast<InAppService::RestoreCallback *>(callback);
        if (!restoreCallback) {
            return;
        }

        InAppService::Error error;
        error.code = code;
        error.message = Utils::toString(message);

        (*restoreCallback)(error);

        delete restoreCallback;
    }

    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativePurchaseCallback(JNIEnv * env, jlong callback, jobject purchase, jint code, jstring message)
    {
        InAppService::PurchaseCallback * purchaseCallback = reinterpret_cast<InAppService::PurchaseCallback *>(callback);
        if (!purchaseCallback) {
            return;
        }

        InAppPurchase result = toInAppPurchase(purchase);

        InAppService::Error error;
        error.code = code;
        error.message = Utils::toString(message);

        (*purchaseCallback)(result, error);

        delete purchaseCallback;
    }

    void Java_com_ludei_inapps_cpp_InAppServiceBridge_nativeConsumeCallback(JNIEnv * env, jlong callback, jint consumed, jint code, jstring message)
    {
        InAppService::ConsumeCallback * consumeCallback = reinterpret_cast<InAppService::ConsumeCallback *>(callback);
        if (!consumeCallback) {
            return;
        }

        InAppService::Error error;
        error.code = code;
        error.message = Utils::toString(message);

        (*consumeCallback)(consumed, error);

        delete consumeCallback;
    }
}
