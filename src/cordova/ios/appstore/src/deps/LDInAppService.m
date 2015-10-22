#import "LDInAppService.h"
#import <StoreKit/StoreKit.h>

#define MAKE_ERROR(n, msg) [[NSError alloc] initWithDomain:@"LDInAppService" code:n userInfo:@{NSLocalizedDescriptionKey:msg}]

#pragma mark Data classes

@implementation LDInAppProduct

+(LDInAppProduct *) fromSKProduct:(SKProduct *) sk
{
    LDInAppProduct * result = [[LDInAppProduct alloc] init];
    result.productId = sk.productIdentifier;
    result.localizedTitle = sk.localizedTitle;
    result.localizedDescription = sk.localizedDescription;
    result.price = sk.price.doubleValue;
    NSNumberFormatter* currencyFormatter = [[NSNumberFormatter alloc] init];
    [currencyFormatter setNumberStyle:NSNumberFormatterCurrencyStyle];
    [currencyFormatter setLocale:sk.priceLocale];
    result.localizedPrice = [currencyFormatter stringFromNumber:sk.price];
    result.currency = [sk.priceLocale objectForKey:NSLocaleCurrencyCode];
    return result;
}

-(NSDictionary *) toDictionary
{
    return @{@"productId": _productId ?: @"", @"title":_localizedTitle ?: @"", @"description": _localizedDescription ?: @"",
             @"price" : [NSNumber numberWithDouble:_price], @"localizedPrice": _localizedPrice ?: @"", @"currency" : _currency ?: @""};
}

+(instancetype) fromDictionary:(NSDictionary *) dic
{
    LDInAppProduct * result = [[LDInAppProduct alloc] init];
    if (!dic || ![dic isKindOfClass:[NSDictionary class]]) {
        dic = @{};
    }
    result.productId = [dic objectForKey:@"productId"] ?: @"";
    result.localizedTitle = [dic objectForKey:@"title"] ?: @"";
    result.localizedDescription = [dic objectForKey:@"description"] ?: @"";
    result.localizedPrice = [dic objectForKey:@"localizedPrice"] ?: @"";
    NSNumber * number = [dic objectForKey:@"price"];
    if (number && [number isKindOfClass:[NSNumber class]]) {
        result.price = [number doubleValue];
    }
    result.currency = [dic objectForKey:@"currency"] ?: @"";
    return result;
}

@end

@implementation LDInAppPurchase

+(LDInAppPurchase *) fromSKTransaction:(SKPaymentTransaction *) transaction
{
    LDInAppPurchase * result = [[LDInAppPurchase alloc] init];
    result.productId = transaction.payment.productIdentifier;
    result.purchaseDate = transaction.transactionDate;
    result.transactionId = transaction.transactionIdentifier;
    result.quantity = transaction.payment.quantity;
    return result;
}

-(NSDictionary *) toDictionary
{
    return @{@"productId": _productId ?: @"", @"transactionId":_transactionId ?: @"", @"quantity": [NSNumber numberWithInteger:_quantity],
             @"date": [NSNumber numberWithInteger:_purchaseDate ? [_purchaseDate timeIntervalSince1970] : 0]};
}

@end

#pragma mark Helper Classes

@interface LDInAppFetchDelegate: NSObject<SKProductsRequestDelegate>
@property (nonatomic, strong) LDFetchProductsCallback completion;
@end

@implementation LDInAppFetchDelegate

- (void)productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response
{
    NSError * error = nil;
    if (response.invalidProductIdentifiers.count > 0) {
        NSString * msg = @"Invalid products: ";
	for (NSString * pid in response.invalidProductIdentifiers) {
	    msg = [msg stringByAppendingString:pid];
	    msg = [msg stringByAppendingString:@","];
	}
	error = MAKE_ERROR(0, msg);
    }
    _completion(response.products, error);
    [self dispose:request];
}

- (void)request:(SKRequest *)request didFailWithError:(NSError *)error {
    _completion(nil, error);
    [self dispose:request];
}

- (void)dispose:(SKRequest*)request {
      request.delegate = nil;
      LDInAppFetchDelegate * this = self;
      //simulate CFAutoRelease for iOS 6
      dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 10 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
	  CFRelease((__bridge CFTypeRef)(this));
      });
}

@end



#pragma mark Store Service Implementation

@interface LDInAppService () <SKPaymentTransactionObserver>
@end


@implementation LDInAppService
{
    NSMutableArray * _purchaseObservers;
    NSMutableDictionary * _cachedSKProducts;
    NSMutableDictionary * _purchaseCompletions;
    BOOL _started;
    void (^_restoreCallback)(NSError * error);
    LDValidationHandler _validationHandler;
}

-(instancetype) init
{
    if (self = [super init]) {
        [[SKPaymentQueue defaultQueue] addTransactionObserver:self];
        _purchaseObservers = [[NSMutableArray alloc] init];
        _cachedSKProducts = [[NSMutableDictionary alloc] init];
        _purchaseCompletions = [[NSMutableDictionary alloc] init];
        _autoFinishPurchases = YES;
        _products = [LDInAppService loadProductsFromCache];
    }
    return self;
}

-(void) dealloc
{
    [[SKPaymentQueue defaultQueue] removeTransactionObserver:self];
}

-(void) addPurchaseObserver:(id<LDInAppPurchaseObserver>) observer
{
    if (![_purchaseObservers containsObject:observer]) {
        [_purchaseObservers addObject:observer];
    }
}

-(void) removePurchaseObserver:(id<LDInAppPurchaseObserver>) observer
{
    [_purchaseObservers removeObject:observer];
}

-(void) start
{
    if (_started) {
        return;
    }
    _started = YES;
    
    //Process missed transactions that happened before the user set up the observers or the validation handler
    NSArray * missedTransactions = [[SKPaymentQueue defaultQueue].transactions copy];
    for (SKPaymentTransaction *transaction in missedTransactions) {
        [self notifyPurchaseStarted:transaction.payment.productIdentifier];
        switch (transaction.transactionState) {
            case SKPaymentTransactionStatePurchased:
            case SKPaymentTransactionStateRestored:
                [self transactionPurchased:transaction];
                break;
            case SKPaymentTransactionStateFailed:
                [self transactionFailed:transaction withError:transaction.error];
                break;
            default:
                break;
        }
    }
}

-(LDInAppProduct*) productForId:(NSString *) identifier
{
    for (LDInAppProduct * product in _products) {
        if ([product.productId isEqualToString:identifier]) {
            return product;
        }
    }
    return nil;
}

-(BOOL) isPurchased:(NSString *) productId
{
    return [self stockOfProduct:productId] > 0;
}

-(NSInteger) stockOfProduct:(NSString *) productId
{
    NSInteger stock = 0;
    [LDInAppService getStockFromKeychain:productId quantity:&stock];
    return stock;
}

-(void) fetchProducts: (NSArray *) productIds completion:(void(^)(NSArray * products, NSError * error)) completion
{
    SKProductsRequest * request = [[SKProductsRequest alloc] initWithProductIdentifiers:[NSSet setWithArray:productIds]];
    LDInAppFetchDelegate * delegate = [[LDInAppFetchDelegate alloc] init];
    delegate.completion = ^(NSArray * skProducts, NSError * error) {
        NSMutableArray * products = [[NSMutableArray alloc] init];
        for (SKProduct * sk in skProducts) {
            [_cachedSKProducts setObject:sk forKey:sk.productIdentifier];
            LDInAppProduct * product = [LDInAppProduct fromSKProduct:sk];
            [products addObject:product];
            [self addProduct:product]; //local cache
        }
        [LDInAppService saveProductsToCache:_products];
        if (completion) {
            completion(products, error);
        }
    };
    CFRetain((__bridge CFTypeRef)(delegate));
    request.delegate = delegate;
    [request start];
}

-(void) restorePurchases:(void(^)(NSError * error)) completion
{
    _restoreCallback = completion;
    [[SKPaymentQueue defaultQueue] restoreCompletedTransactions];
}

-(BOOL) canPurchase
{
    return [SKPaymentQueue canMakePayments];
}

-(void) purchase:(NSString *) productId completion:(void(^)(NSError * error)) completion;
{
    [self purchase:productId quantity:1 completion:completion];
}

-(void) purchase:(NSString *) productId quantity:(NSInteger) quantity completion:(void(^)(NSError * error)) completion;
{
    if (completion) {
        [_purchaseCompletions setObject:completion forKey:productId];
    }
    SKProduct * sk = [_cachedSKProducts objectForKey:productId];
    if (sk) {
        SKMutablePayment * payment = [SKMutablePayment paymentWithProduct:sk];
        payment.quantity = quantity;
        [[SKPaymentQueue defaultQueue] addPayment:payment];
    }
    else {
        [self fetchProducts:@[productId] completion:^(NSArray *products, NSError *error) {
            if (products.count > 0) {
                SKMutablePayment * payment = [SKMutablePayment paymentWithProduct:[_cachedSKProducts objectForKey:productId]];
                payment.quantity = quantity;
                [[SKPaymentQueue defaultQueue] addPayment:payment];
                return;
            }
            [self notifyPurchaseFailed:productId withError:error ?: MAKE_ERROR(0, @"ProductId not found")];
        }];
    }
}

-(NSInteger) consume:(NSString *) productId quantity:(NSInteger) quantity
{
    NSInteger stock = [self stockOfProduct:productId];
    NSInteger newStock = MAX(stock - MAX(quantity, 0), 0);
    if (stock != newStock) {
        [LDInAppService saveStockToKeychain:productId quantity:newStock];
    }
    return stock - newStock;
}

-(void) finishPurchase:(NSString *) transactionId
{
    for (SKPaymentTransaction * transaction in SKPaymentQueue.defaultQueue.transactions) {
        if ([transaction.transactionIdentifier isEqualToString:transactionId]) {
            [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
            break;
        }
    }
}

-(void) transactionPurchased:(SKPaymentTransaction *) transaction
{
    LDValidationCompletion completion = ^(NSError * error) {
        if (error) {
            //Validation failed. We don't finish the purchase, so the transaction can be tried later
            [self notifyPurchaseFailed:transaction.payment.productIdentifier withError:error];
        }
        else {
            NSString * productId = transaction.payment.productIdentifier;
            NSInteger stock = 0;
            [LDInAppService getStockFromKeychain:productId quantity:&stock];
            stock+= transaction.payment.quantity;
            [LDInAppService saveStockToKeychain:productId quantity:stock];
            if (_autoFinishPurchases) {
                [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
            }
            [self notifyPurchaseCompleted:[LDInAppPurchase fromSKTransaction:transaction]];
        }
        
    };
    if (_validationHandler) {
        NSData * receipt = transaction.transactionReceipt;
        _validationHandler(receipt, transaction.payment.productIdentifier, completion);
    }
    else {
        completion(nil);
    }
}

-(void) transactionFailed: (SKPaymentTransaction *) transaction withError:(NSError *) error
{
    [self notifyPurchaseFailed:transaction.payment.productIdentifier withError:error];
    [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
}

-(void) setValidationHandler:(LDValidationHandler) handler
{
    _validationHandler = handler;
}
-(void) setLudeiServerValidationHandler
{
    _validationHandler = ^(NSData * transactionReceipt, NSString * productId, LDValidationCompletion completion) {
        
        NSMutableDictionary * requestBody = [NSMutableDictionary dictionary];
        [requestBody setObject:[NSNumber numberWithInt:0] forKey:@"os"];
        [requestBody setObject:[[NSBundle mainBundle] bundleIdentifier] forKey:@"bundleId"];
        [requestBody setObject:@{@"receipt":[LDInAppService dataToBase64:transactionReceipt]} forKey:@"data"];
        [requestBody setObject:@"quohToh1pieF7ohmUieile6Koodae9ak6L0EeteeYiedaor8iCh5oowa" forKey:@"api_key"];
        
        NSURL * url = [NSURL URLWithString:@"https://cloud.ludei.com/api/v2/verify-purchases/"];
        NSMutableURLRequest * request = [NSMutableURLRequest requestWithURL:url];
        [request setHTTPMethod:@"POST"];
        [request setHTTPBody:[NSJSONSerialization dataWithJSONObject:requestBody options:0 error:nil]];

        [NSURLConnection sendAsynchronousRequest:request queue:[[NSOperationQueue alloc] init] completionHandler:^(NSURLResponse *response, NSData *data, NSError *connectionError) {
            
            if (connectionError) {
                completion(connectionError);
                return;
            }
            NSError * error = nil;
            NSDictionary * json = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
            if (error) {
                completion(error);
                return;
            }
            NSInteger status = [[json objectForKey:@"status"] integerValue];
            if (status != 0) {
                completion(MAKE_ERROR(status, [json objectForKey:@"errorMessage"] ?: @"Invalid response status"));
                return;
            }
            
            // In IOS the verification receipt only contains information for one purchase, but for Android campatibility purposes we return an array of orders.
            NSArray * orders = [json objectForKey:@"orders"];
            if (orders.count == 0) {
                completion(MAKE_ERROR(0, @"Empty response"));
                return;
            }
            
            NSDictionary * order = [orders objectAtIndex:0];
            if ([productId isEqualToString:[order objectForKey:@"productId"]]) {
                completion(nil);
            }
            else {
                completion(MAKE_ERROR(0, @"ProductId doest not match"));
            }
            
        }];
    };
}

#pragma mark SKPaymentTransactionObserver

- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray *)transactions NS_AVAILABLE_IOS(3_0)
{
    if (!_started) {
        return;
    }
    
    for (SKPaymentTransaction *transaction in transactions) {
        switch (transaction.transactionState) {
            case SKPaymentTransactionStatePurchased:
                [self transactionPurchased:transaction];
                break;
            case SKPaymentTransactionStateFailed:
                [self transactionFailed:transaction withError:transaction.error];
                break;
            case SKPaymentTransactionStateRestored:
                [self transactionPurchased:transaction];
                break;
            case SKPaymentTransactionStatePurchasing:
                [self notifyPurchaseStarted:transaction.payment.productIdentifier];
            default:
                break;
        }			
    }
}

// Sent when transactions are removed from the queue (via finishTransaction:).
- (void)paymentQueue:(SKPaymentQueue *)queue removedTransactions:(NSArray *)transactions NS_AVAILABLE_IOS(3_0)
{
    
}

// Sent when an error is encountered while adding transactions from the user's purchase history back to the queue.
- (void)paymentQueue:(SKPaymentQueue *)queue restoreCompletedTransactionsFailedWithError:(NSError *)error
{
    if (_restoreCallback) {
        _restoreCallback(error);
        _restoreCallback = nil;
    }
    
}

// Sent when all transactions from the user's purchase history have successfully been added back to the queue.
- (void)paymentQueueRestoreCompletedTransactionsFinished:(SKPaymentQueue *)queue
{
    if (_restoreCallback) {
        _restoreCallback(nil);
        _restoreCallback = nil;
    }
}

#pragma mark Notify helpers

-(void) notifyPurchaseStarted:(NSString *) productId
{
    for (id observer in _purchaseObservers) {
        if ([observer respondsToSelector:@selector(inAppService:didStartPurchase:)]) {
            [observer inAppService:self didStartPurchase:productId];
        }
    }
}

-(void) notifyPurchaseFailed:(NSString *) productId withError:(NSError *) error
{
    for (id observer in _purchaseObservers) {
        if ([observer respondsToSelector:@selector(inAppService:didFailPurchase:withError:)]) {
            [observer inAppService:self didFailPurchase:productId withError:error];
        }
    }
    void (^completion)(NSError * error) = [_purchaseCompletions objectForKey:productId];
    if (completion) {
        completion(error);
        [_purchaseCompletions removeObjectForKey:productId];
    }
}

-(void) notifyPurchaseCompleted:(LDInAppPurchase *) purchase
{
    for (id observer in _purchaseObservers) {
        if ([observer respondsToSelector:@selector(inAppService:didCompletePurchase:)]) {
            [observer inAppService:self didCompletePurchase:purchase];
        }
    }
    void (^completion)(NSError * error) = [_purchaseCompletions objectForKey:purchase.productId];
    if (completion) {
        completion(nil);
        [_purchaseCompletions removeObjectForKey:purchase.productId];
    }
}

#pragma mark Keychain Storage Utilities

+(void) saveStockToKeychain:(NSString*)productId quantity:(NSInteger) quantity
{
    NSString * serviceName = [NSString stringWithFormat:@"store_%@", productId];
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword),  kSecClass, serviceName, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, nil];
    // Remove any old values from the keychain
    OSStatus err = SecItemDelete((__bridge CFDictionaryRef) dict);

    // Create dictionary of parameters to add
    NSData* quantityData = [[NSString stringWithFormat:@"%ld", (long)quantity] dataUsingEncoding:NSUTF8StringEncoding];
    dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, serviceName, kSecAttrServer, quantityData, kSecValueData, @"purchase", kSecAttrAccount, nil];
    
    // Try to save to keychain
    err = SecItemAdd((__bridge CFDictionaryRef) dict, NULL);
}

+(BOOL) getStockFromKeychain:(NSString *) productId quantity:(NSInteger *) quantity {
    
    NSString * serviceName = [NSString stringWithFormat:@"store_%@", productId];
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword),  kSecClass, serviceName, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];
    
    // Look up server in the keychain
    NSDictionary* found = nil;
    SecItemCopyMatching((__bridge CFDictionaryRef) dict, (void*) &found);
    if (!found) return NO;
    
    // Found
    NSString * quanityValue = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];
    *quantity = [quanityValue integerValue];
    
    return YES;
}

+(NSString *) dataToBase64:(NSData *) data
{
    const char * BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789+/";
    unsigned char const* bytes_to_encode = data.bytes;
    NSUInteger in_len = data.length;
    NSMutableString * ret = [[NSMutableString alloc] init];
    int i = 0;
    int j = 0;
    unsigned char char_array_3[3];
    unsigned char char_array_4[4];
    
    while (in_len--) {
        char_array_3[i++] = *(bytes_to_encode++);
        if (i == 3) {
            char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
            char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
            char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
            char_array_4[3] = char_array_3[2] & 0x3f;
            
            for(i = 0; (i <4) ; i++)
                [ret appendFormat:@"%c", BASE64_CHARS[char_array_4[i]]];
            i = 0;
        }
    }
    
    if (i)
    {
        for(j = i; j < 3; j++)
            char_array_3[j] = '\0';
        
        char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
        char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
        char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
        char_array_4[3] = char_array_3[2] & 0x3f;
        
        for (j = 0; (j < i + 1); j++)
            [ret appendFormat:@"%c", BASE64_CHARS[char_array_4[j]]];
        
        while((i++ < 3))
            [ret appendFormat:@"%c", '='];
    }
    
    return ret;
}

#pragma mark Products Cache

#define PRODUCTS_CACHE_KEY @"LDInApp_cachedProducts"

-(void) addProduct:(LDInAppProduct * ) product {
    
    for (LDInAppProduct * p in _products) {
        if ([p.productId isEqualToString:product.productId]) {
            [_products removeObject:p];
            break;
        }
    }
    [_products addObject:product];
}

+(void) saveProductsToCache:(NSArray *) products {
    NSMutableArray * array = [NSMutableArray array];
    for (LDInAppProduct * product in products) {
        [array addObject:[product toDictionary]];
    }
    [[NSUserDefaults standardUserDefaults] setObject:array forKey:PRODUCTS_CACHE_KEY];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

+(NSMutableArray *) loadProductsFromCache {
    
    NSMutableArray * result = [NSMutableArray array];
    NSArray * array = [[NSUserDefaults standardUserDefaults] objectForKey:PRODUCTS_CACHE_KEY] ?: @[];
    if (array && [array isKindOfClass:[NSArray class]]) {
        for (NSDictionary * dic in array) {
            [result addObject:[LDInAppProduct fromDictionary:dic]];
        }
    }
    return result;
}

@end
