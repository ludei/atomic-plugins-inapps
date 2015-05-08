#import <UIKit/UIKit.h>

@interface LDInAppProduct: NSObject

/**
 *  The id of a product.
 */
@property (nonatomic, strong) NSString * productId;

/**
 *  The title of a product.
 */
@property (nonatomic, strong) NSString * localizedTitle;

/**
 *  The description of a product.
 */
@property (nonatomic, strong) NSString * localizedDescription;

/**
 *  The price of a product.
 */
@property (nonatomic, assign) CGFloat price;

/**
 *  The price of the product in local currency.
 */
@property (nonatomic, strong) NSString * localizedPrice;

@property (nonatomic, strong) NSString * currency;

-(NSDictionary *) toDictionary;
+(instancetype) fromDictionary:(NSDictionary *) dic;

@end

@interface LDInAppPurchase : NSObject

/**
 *  The transaction id.
 */
@property (nonatomic, strong) NSString * transactionId;

/**
 *  The product id.
 */
@property (nonatomic, strong) NSString * productId;

/**
 *  The date when the purchase was completed.
 */
@property (nonatomic, strong) NSDate * purchaseDate;

/**
 *  The quantity of the product purchased.
 */
@property (nonatomic, assign) NSInteger quantity;

-(NSDictionary *) toDictionary;

@end

@class LDInAppService;

@protocol LDInAppPurchaseObserver
@optional

/**
 *  Triggered when a purchase has started.
 *
 *  @param service   The service.
 *  @param productId The id of the product purchased.
 */
-(void) inAppService:(LDInAppService *) service didStartPurchase:(NSString *) productId;

/**
 *  Triggered when a purchase has failed.
 *
 *  @param service   The service.
 *  @param productId The product id.
 *  @param error     The reported error.
 */
-(void) inAppService:(LDInAppService *) service didFailPurchase:(NSString *) productId withError:(NSError *) error;

/**
 *  Triggered when a purchase has finished successfully.
 *
 *  @param service  The service.
 *  @param purchase The purchase.
 */
-(void) inAppService:(LDInAppService *) service didCompletePurchase:(LDInAppPurchase *) purchase;

@end

@interface LDInAppService: NSObject

/**
 *  The cached products.
 */
@property (nonatomic, readonly) NSMutableArray * products;

/**
 *  Defines if purchases should finish automatically or not.
 */
@property (nonatomic, assign) BOOL autoFinishPurchases;


typedef void(^LDFetchProductsCallback)(NSArray * products, NSError * error);
typedef void(^LDValidationCompletion)(NSError * error);
typedef void(^LDValidationHandler)(NSData * validationReceipt, NSString * productId, LDValidationCompletion completion);

-(instancetype) init;

/**
 *  Adds an observer.
 *
 *  @param observer The observer.
 */
-(void) addPurchaseObserver:(id<LDInAppPurchaseObserver>) observer;

/**
 *  Removes an observer.
 *
 *  @param observer The observer.
 */
-(void) removePurchaseObserver:(id<LDInAppPurchaseObserver>) observer;

/**
 *  Starts processing transactions and receiving LDInAppPurchaseObserver notifications.
 *  You have to call this method when your LDInAppPurchaseObservers are ready.
 */
-(void) start;

/**
 *  Requests information about products from Apple Store.
 *  Products are saved in a local DB if saveProducts property is set to YES.
 *
 *  @param productIds The ids of the products.
 *  @param completion Completion.
 */
-(void) fetchProducts: (NSArray *) productIds completion:(LDFetchProductsCallback) completion;

/**
 *  Gets product info for product indetifier
 *  It uses a local cache, so fetchProducts have to be called before if products are not saved from previus executions.
 *
 *  @param identifier The id of the product to get.
 *
 *  @return The product that has that id.
 */
-(LDInAppProduct*) productForId:(NSString *) identifier;

/**
 *  Returns YES if the product is purchased. 
 *  Uses the local purchase database, so it only works if savePurchases property is enabled.
 *
 *  @param productId The product id of the product to check.
 *
 *  @return True if the product is purchased.
 */
-(BOOL) isPurchased:(NSString *) productId;

/**
 *  Returns the quantity of available items for a specific productId. 
 *  Uses the local purchase database, so it only works if savePurchases property is enabled. 
 *  For consumable products it returns the avaiable items. 
 *  For non consumable products it returns 1 i purchased, 0 otherwise.
 *
 *  @param productId The product id of the product to check.
 *
 *  @return The stock of the given product. 
 */
-(NSInteger) stockOfProduct:(NSString *) productId;

/**
 *  Restores already completed transactions and purchases.
 *  LDInAppPurchaseObserver observer is called again for each transaction.
 *
 *  @param completion Completion.
 * 
 *  @see LDInAppPurchaseObserver
 */
-(void) restorePurchases:(void(^)(NSError * error)) completion;

/**
 *  Returns YES if the device is allowed to make payments.
 *
 *  @return True if the device is allowed to make payments.
 */
-(BOOL) canPurchase;

/**
 *  Purchases a product.
 *
 *  @param productId  The id of the product to purchase.
 *  @param completion An error if the process fails.
 *
 *  @see LDInAppPurchaseObserver
 */
-(void) purchase:(NSString *) productId completion:(void(^)(NSError * error)) completion;

/**
 *  Purchases a quantity of a specific product.
 *
 *  @param productId  The id of the purchased product.
 *  @param quantity   The quatity to purchase.
 *  @param completion An error if the process fails.
 *
 *  @see LDInAppPurchaseObserver
 */
-(void) purchase:(NSString *) productId quantity:(NSInteger) quantity completion:(void(^)(NSError * error)) completion;

/**
 *  Consumes a quantity of consumable products.
 *  Uses the local purchase database, so it only works if savePurchases property is enabled.
 *
 *  @param productId The product id.
 *  @param quantity  The quantity of the product.
 *
 *  @return The quantity of consumed purchases.
 */
-(NSInteger) consume:(NSString *) productId quantity:(NSInteger) quantity;

/**
 *  Removes a finished purchase transaction from the queue.
 *
 *  @param transactionId The transaction id.
 *  @see autoFinishPurchases. If the property is set finishPurchase is automatically called.
 */
-(void) finishPurchase:(NSString *) transactionId;

/**
 *  Sets a custom purchase validation handler.
 *  Purchases are always validated to TRUE by default.
 *  Set a custom validation handler to use you own custom server to validate purchases.
 *
 *  @param handler The custom validation handler.
 */
-(void) setValidationHandler:(LDValidationHandler) handler;

/**
 *  Use Ludei's server to validate purchases.
 *  To enable validatioon using Ludei's server you first need to create an account in Ludei's Cloud server and create a project with you bundleId.
 */
-(void) setLudeiServerValidationHandler;

@end
