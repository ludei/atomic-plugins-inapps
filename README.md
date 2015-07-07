#Atomic Plugins for In-App Purchases

This repo contains In-App purchase APIs designed using the [Atomic Plugins](#about-atomic-plugins) paradigm. You can integrate IAPs in your app and take advantage of all the features provided: elegant API, local and server-side receipt validation, secure consumable and non-consumable purchase tracking, local product cache and more. The API is already available in many languagues and we plan to add more in the future:

  * [Objective-C API for pure iOS/Mac apps](#ios-api)
  * [Java API for pure Android apps](#android-api)
  * [JavaScript API for Cordova or Cocoon based Apps](#javascript-api)
  * [C++ API for C++ based apps and games](#c-api)

Currently there are 3 in-app purchase providers implemented but new ones can be easily added:

* Apple AppStore (iOS/Mac)
* GooglePlay
* Amazon AppStore. 

You can contribute and help to create more awesome plugins.

##About Atomic Plugins

Atomic Plugins provide an elegant and minimalist API and are designed with portability in mind from the beginning. Framework dependencies are avoided by design so the plugins can run on any platform and can be integrated with any app framework or game engine. 

#Provided APIs

* [iOS API](#ios-api)
  * [API Reference](#api-reference)
  * [Introduction](#introduction)
  * [Setup your project](#setup-your-project)
  * [Example](#example)
* [Android API](#android-api)
  * [API Reference](#api-reference-1)
  * [Introduction](#introduction-1)
  * [Setup your project](#setup-your-project-1)
  * [Example](#example-1)
* [JavaScript API](#javascript-api)
  * [API Reference](#api-reference-2)
  * [Introduction](#introduction-2)
  * [Setup your project](#setup-your-project-2)
  * [Example](#example-2)
* [C++ API](#c-api)
  * [API Reference](#api-reference-3)
  * [Introduction](#introduction-3)
  * [Setup your project](#setup-your-project-3)
  * [Example](#example-3)
  
##iOS API:

###API Reference

See [API Documentation](http://ludei.github.io/atomic-plugins-inapps/dist/doc/ios/html/annotated.html)

See [`LDInAppService.h`](src/atomic/ios/appstore/LDInAppService.h) header file for a complete overview of the capabilities of the class.

See [`InAppTest`](test/ios)  for a complete project that tests all the features provided in the API.

###Introduction 

LDInAppService class provides an easy to use and secure in-app purchase API. Built-in support for local and server-side receipt validation, consumable and non-consumable purchase tracking and local product cache. Completed purchases are secured using Apple's keychain services and are remembered even if the user deletes the app.

###Setup your project

You can use CocoaPods:

    pod 'LDInAppService'

###Example

```objc
LDInAppService * service = [[LDInAppService alloc] init];

[service addPurchaseObserver: self]; //optional delegate to observe purchases
[service start]; //start processing transactions

[service fetchProducts:@[@"productIds"] completion:^(NSArray *products, NSError *error) {
     //fetch products from remote server
}];

service.products; //local cached products

[service purchase:productId quantity:2 completion:^(NSError *error) {
    //purchase consumable or non consumable products
}];
[service consume:productId quantity:1]; //consume product

[service isPurchased:productId]; //check if a productId is purchased
[service stockOfProduct:productId]; //check available stock of consumable products

[service restorePurchases:^(NSError *error) {
     //restore non-consumable purchases
}];
[service setValidationHandler:^(NSData *validationReceipt, NSString *productId, LDValidationCompletion completion) {
    //custom server validation
    completion(nil); //Call completion with nil error if validation succeeds
}];
[service setLudeiServerValidationHandler]; //validate using Ludei's Cloud server
```

##Android API:

###API Reference

See [API Documentation](http://ludei.github.io/atomic-plugins-inapps/dist/doc/android/html/annotated.html)

See [`InAppTest`](test/android) for a complete project that tests all the features provided in the API. In order to test Android In-Apps select the googlePlayRelelease or amazonRelease built variant in Android Studio. You must set your custom productIds in [`MainActivity.java`](test/android/InAppTest/testapp/src/main/java/com/ludei/inapptest/MainActivity.java) and set your bundleId and versionCode in the [`build.gradle`](test/android/InAppTest/testapp/build.gradle)

You also have to configure your release signingConfig. Add these properties to your global gradle.properties file

    STORE_FILE=/Users/user/.android/company.keystore
    STORE_PASSWORD=xxxxx
    KEY_ALIAS=xxxxxxx
    KEY_PASSWORD=xxxxx

###Introduction 

InAppService interface provides an easy to use and secure in-app purchase API. Built-in support for local and server-side receipt validation, consumable and non-consumable purchase tracking and local product cache. Single API for multiple IAP providers.

## Setup your project

Releases are deployed to Maven Central. You only have to add the desired dependencies in your build.gradle:

    dependencies {
        compile 'com.ludei.inapps.googleplay:1.0.0' //Google Play IAP Provider
        compile 'com.ludei.inapps.amazon:1.0.0' //Amazon AppStore IAP Provider
    }

###Example

```java
//create the service instance using the desired provider
InAppService service =  new GooglePlayInAppService(context);
//service =  new AmazonInAppService(context);

service.fetchProducts(productIds, new InAppService.FetchCallback() {
    public void onComplete(List<InAppProduct> products, InAppService.Error error) {
        //fetch products from store
    }
});

service.getProducts(); // Local cached products

service.purchase(productId, new InAppService.PurchaseCallback() {
    public void onComplete(InAppPurchase purchase, InAppService.Error error) {
        //purchase product
    }
});

service.consume(productId, 1, new InAppService.ConsumeCallback() {
    public void onComplete(int consumed, InAppService.Error error) {
        //consume product
    }
});

service.isPurchased(productId); //check if a product is purchased
service.stockOfProduct(productId); //check available stock of consumable products 

service.restorePurchases(new InAppService.RestoreCallback() {
    public void onComplete(InAppService.Error error) {
        //Restore completed purchases
    }
});

service.setValidationHandler(new InAppService.ValidationHandler() {
    public void onValidate(String receipt, String productId, InAppService.ValidationCompletion completion) {
        //custom server validation
    }
});

service.setLudeiServerValidationHandler(); //Validate using Ludei's Cloud server
```

Remember to notify onActivityResult and onDestroy events

```java

@Override
public void onActivityResult(int requestCode, int resultCode, Intent data)
{
    boolean handled = service.onActivityResult(requestCode, resultCode, data);
    if (!handled) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}

@Override
public void onDestroy()
{
    service.onDestroy();
    super.onDestroy();
}

```

##JavaScript API:

###API Reference

See [API Documentation](http://ludei.github.io/cocoon-common/dist/doc/js/Cocoon.InApp.html)

For a complete project that tests all the features provided in the API run the following command:

    gulp create-cordova

A cordova project is created in test/cordova/InAppTest

###Introduction 

Cocoon.InApp class provides a multiplatform, easy to use and secure in-app purchase API. Built-in support for local and server-side receipt validation, consumable and non-consumable purchase tracking and local product cache. Single JavaScript API for multiple IAP providers.

###Setup your project

Releases are deployed to Cordova Plugin Registry. You only have to install the desired plugins using Cordova CLI, CocoonJS CLI or Ludei's Cocoon Cloud Server.

    cordova plugin add cocoon-plugin-inapps-ios-appstore;
    cordova plugin add cocoon-plugin-inapps-android-googleplay;
    cordova plugin add cocoon-plugin-inapps-android-amazon;

The following JavaScript file is included automatically:

[`cocoon_inapps.js`](src/cordova/js/cocoon_inapps.js)

###Example

```javascript
// Basic usage, register callbacks first
var service = Cocoon.InApp;
service.on("purchase", {
    start: function(productId) {
        console.log("purchase started " + productId);
    },
    error: function(productId, error) {
        console.log("purchase failed " + productId + " error: " + JSON.stringify(error));
    },
    complete: function(purchase) {
        console.log("purchase completed " + JSON.stringify(purchase));
    }
});

// Service initialization
service.initialize({
        autofinish: true
    }, function(error){

    }
);

service.fetchProducts(productIds, function(products, error){
    //Fetch products from the server
});   

service.getProducts(); //Local cached products

service.purchase(productId, 1, function(error) { // Optional sugar callback
    //purchase product
});

service.consume(productId, 3, function(error) {
    //consume product
});

service.isPurchased(productId); //check if a product is purchased
service.stockOfProduct(productId); //check available stock of consumable products

service.setValidationHandler(function(receipt, productId, completion){
     ... //Custom server validation code
     completion(true); //call completion function with true param if validation succeeds
});

service.setLudeiServerValidationHandler(); //validate using Ludei's Cloud server
```

##C++ API:

###API Reference

See [API Documentation](http://ludei.github.io/atomic-plugins-inapps/dist/doc/cpp/html/annotated.html)

See [`InAppService.h`](src/cpp/InAppService.h) header file for a complete overview of the capabilities of the class.

See [`InAppTest`](test/cpp) for a complete project (cocos2dx game) that integrates the C++ IAP API.

###Introduction 

InAppService class provides a multiplatform, easy to use and secure in-app purchase API. Built-in support for local and server-side receipt validation, consumable and non-consumable purchase tracking and local product cache. Single C++ API for multiple IAP providers.

###Setup your project

You can download prebuilt headers and static libraries from [Releases page](https://github.com/ludei/atomic-plugins-inapps/releases)

These static libraries provide the bindings between C++ and the native platform (iOS, Android, WP, etc). You might need to add some platform dependent libraries to your project (some jar files or gradle dependecies for example). See [`InAppTest`](test/cpp) for an already setup C++ multiplatform project.

####Special setup required for Android

There isn't a portable and realiable way to get the current Activity and life cycle events on Android and we don't want to depend on specific game engine utility APIs. C++ and Java bridge is implemented using the [SafeJNI](https://github.com/MortimerGoro/SafeJNI) utility. Atomic Plugins take advantage of this class and use it also as a generic Activity and Life Cycle Event notification provider. See the following code to set up the activity for atomic plugins and to notify Android life cycle events.

```java
@Override
public void onCreate(Bundle savedInstanceState) {
    //set the activity for atomic plugins and load safejni.so
    SafeJNI.INSTANCE.setActivity(this); 
    super.onCreate(savedInstanceState);
}
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    //notify onActivityResult to atomic plugins
    SafeJNI.INSTANCE.onActivityResult(requestCode, resultCode, data);
}
```

Optionally (but recommended) you can use setJavaToNativeDispatcher to configure the thread in which async callbacks should be dispatched. By default callbacks are dispatched in the UI Thread. For example the following dispatcher is used in the Cocos2dx game engine test project.

```java
@Override
public Cocos2dxGLSurfaceView onCreateView() {
    final Cocos2dxGLSurfaceView surfaceView = super.onCreateView();
    SafeJNI.INSTANCE.setJavaToNativeDispatcher(new SafeJNI.JavaToNativeDispatcher() {
        @Override
        public void dispatch(Runnable runnable) {
            surfaceView.queueEvent(runnable);
        }
    });
    return surfaceView;
}
```
#####Signing a build for release

Set your bundleId and versionCode in the [`build.gradle`](test/cpp/proj.android/app/build.gradle)

You also have to configure your release signingConfig. Add these properties to your global gradle.properties file

    STORE_FILE=/Users/user/.android/company.keystore
    STORE_PASSWORD=xxxxx
    KEY_ALIAS=xxxxxxx
    KEY_PASSWORD=xxxxx

###Example

```cpp

//Easy to use static method to instantiate a new service
//You can pass a specific InAppProvider if you have many providers linked in your app and you want to choose one of them at runtime
InAppService * service = InAppService::create();

service->addPurchaseObserver(observer); //optional purchase observer;
service->start(); //start processing transactions

service->fetchProducts(productIds, [=](const std::vector<InAppProduct> & products, const InAppService::Error & error){
    if (error.empty()) {
        //show error
        return;
    }
    // fetch products from remote server
});

service->purchase(productId, [=](const InAppPurchase & purchase, const InAppService::Error & error){
    //purchase product
});

service->consume(productId, 1, [=](int32_t consumed, const InAppService::Error & error){
    //consume product
});

service->isPurchased(productId); //check if a productId is purchased
service->stockOfProduct(productId); //check available stock of consumable products

service->restorePurchases([=](const InAppService::Error & error){
    //restore purchases
});

service->setValidationHandler([=](const string & receipt, const string & productId, const ValidationCompletion & completion){
    //custom server validation code
    completion(InAppService::Error()); //call completion with empty error if validation succeeds
});

service->setLudeiServerValidationHandler(); //validate using Ludei's Cloud server

//delete the service when you are done. You can wrap it into a Smart Pointer if you want.
delete service; 

```

#License

Mozilla Public License, version 2.0

Copyright (c) 2015 Ludei 

See [`MPL 2.0 License`](LICENSE)
