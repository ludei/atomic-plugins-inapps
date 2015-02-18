#import <UIKit/UIKit.h>

@interface LDInAppProduct: NSObject

@property (nonatomic, strong) NSString * productId;
@property (nonatomic, strong) NSString * localizedTitle;
@property (nonatomic, strong) NSString * localizedDescription;
@property (nonatomic, assign) CGFloat price;
@property (nonatomic, strong) NSString * localizedPrice;

-(NSDictionary *) toDictionary;
+(instancetype) fromDictionary:(NSDictionary *) dic;

@end

@interface LDInAppPurchase : NSObject

@property (nonatomic, strong) NSString * transactionId;
@property (nonatomic, strong) NSString * productId;
@property (nonatomic, strong) NSDate * purchaseDate;
@property (nonatomic, assign) NSInteger quantity;

-(NSDictionary *) toDictionary;

@end


@class LDInAppService;

@protocol LDInAppPurchaseObserver
@optional

-(void) inAppService:(LDInAppService *) service didStartPurchase:(NSString *) productId;
-(void) inAppService:(LDInAppService *) service didFailPurchase:(NSString *) productId withError:(NSError *) error;
-(void) inAppService:(LDInAppService *) service didCompletePurchase:(LDInAppPurchase *) purchase;

@end


@interface LDInAppService: NSObject

@property (nonatomic, readonly) NSMutableArray * products;
@property (nonatomic, assign) BOOL autoFinishPurchases;


typedef void(^LDFetchProductsCallback)(NSArray * products, NSError * error);
typedef void(^LDValidationCompletion)(NSError * error);
typedef void(^LDValidationHandler)(NSData * validationReceipt, NSString * productId, LDValidationCompletion completion);

-(instancetype) init;
-(void) addPurchaseObserver:(id<LDInAppPurchaseObserver>) observer;
-(void) removePurchaseObserver:(id<LDInAppPurchaseObserver>) observer;

/**
 * Start processing transactions and receiving LDInAppPurchaseObserver notifications
 * You have to call this method when your LDInAppPurchaseObservers are ready
 **/
-(void) start;
/**
 * Request information about products from Apple Store
 * Products are saved in a local DB if saveProducts property is set to YES
 **/
-(void) fetchProducts: (NSArray *) productIds completion:(LDFetchProductsCallback) completion;
/**
 * Get's product info for product indetifier
 * It uses a local cache, so fetchProducts have to be called before if products are not saved from previus executions
 **/
-(LDInAppProduct*) productForId:(NSString *) identifier;

/**
 * Returns YES if the product is purchased
 * Uses the local purchase database, so it only works if savePurchases property is enabled
 **/
-(BOOL) isPurchased:(NSString *) productId;

/**
 * Returns the quantity of available items for a specific productId
 * Uses the local purchase database, so it only works if savePurchases property is enabled
 * For consumable products it returns the avaiable items
 * For non consumable products it returns 1 i purchased, 0 otherwise
 **/
-(NSInteger) stockOfProduct:(NSString *) productId;
/**
 * Restore already completed transactions and purchases
 * LDInAppPurchaseObserver observer is called again for each transaction
 * @see LDInAppPurchaseObserver
 **/
-(void) restorePurchases:(void(^)(NSError * error)) completion;
/** 
 * returns YES if the device is allowed to make payments
 **/
-(BOOL) canPurchase;
/**
 * Purchase a product
 * @see LDInAppPurchaseObserver
 **/
-(void) purchase:(NSString *) productId completion:(void(^)(NSError * error)) completion;
/**
 * Purchase a quantity of products
 * @see LDInAppPurchaseObserver
 **/
-(void) purchase:(NSString *) productId quantity:(NSInteger) quantity completion:(void(^)(NSError * error)) completion;

/**
 * Consume a quantity of consumable products
 * Uses the local purchase database, so it only works if savePurchases property is enabled
 * @return returne the quantity of consumed purchases
 **/
-(NSInteger) consume:(NSString *) productId quantity:(NSInteger) quantity;

/**
 * Remove a finished purchase transaction from the queue
 * @see autoFinishPurchases. If the property is set finishPurchase is automatically called.
 **/
-(void) finishPurchase:(NSString *) transactionId;

/**
 * Sets a custom purchase validation handler.
 * Purchases are always validated to TRUE by default
 * Set a custom validation handler to use you own custom server to validate purchases
 **/
-(void) setValidationHandler:(LDValidationHandler) handler;
/**
 * Use Ludei's server to validate purchases
 **/
-(void) setLudeiServerValidationHandler;

@end
