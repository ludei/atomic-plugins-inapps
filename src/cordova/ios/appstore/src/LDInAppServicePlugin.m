#import "LDInAppServicePlugin.h"


@implementation LDInAppServicePlugin
{
    LDInAppService * _service;
    NSString * _listenerCallbackId;
    NSMutableDictionary * _validationCompletions; //for custom validation handlers
}

- (void)pluginInitialize
{
    _validationCompletions = [[NSMutableDictionary alloc] init];
    
    _service = [[LDInAppService alloc] init];
    [_service addPurchaseObserver:self];
}

-(void) setListener:(CDVInvokedUrlCommand*) command
{
    _listenerCallbackId = command.callbackId;
}

-(void) initialize:(CDVInvokedUrlCommand*)command
{
    NSDictionary * data = @{@"products": [self productsToJSON:_service.products],
                            @"canPurchase": [NSNumber numberWithBool:[_service canPurchase]] };
    CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:data];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    [_service start];
}

-(void) fetchProducts: (CDVInvokedUrlCommand*)command
{
    id productIds = [command argumentAtIndex:0];
    if (![productIds isKindOfClass:[NSArray class]]) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid argument"] callbackId:command.callbackId];
        return;
    }
    
    [_service fetchProducts:productIds completion:^(NSArray *products, NSError *error) {
        
        CDVPluginResult * result;
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self errorToDic:error]];
        }
        else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:[self productsToJSON:products]];
        }
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}


-(void) getProducts:(CDVInvokedUrlCommand*) command
{
    CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:[self productsToJSON:_service.products]];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

-(void) productForId:(CDVInvokedUrlCommand*)command
{
    NSString * productId = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    LDInAppProduct * product = [_service productForId:productId];
    CDVPluginResult * result;
    if (product) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[product toDictionary]];
    }
    else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

-(void) isPurchased:(CDVInvokedUrlCommand*)command
{
    NSString * productId = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[_service isPurchased:productId]] callbackId:command.callbackId];
}

-(void) stockOfProduct:(CDVInvokedUrlCommand *) command
{
    NSString * productId = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:(int)[_service stockOfProduct:productId]] callbackId:command.callbackId];
}

-(void) restorePurchases:(CDVInvokedUrlCommand *) command
{
    [_service restorePurchases:^(NSError *error) {
        
        CDVPluginResult * result;
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self errorToDic:error]];
        }
        else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

-(void) canPurchase:(CDVInvokedUrlCommand *) command
{
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[_service canPurchase]] callbackId:command.callbackId];
}

-(void) purchase:(CDVInvokedUrlCommand *) command
{
    NSString * productId = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    NSNumber * quantity = [command argumentAtIndex:1 withDefault:[NSNumber numberWithInt:1] andClass:[NSNumber class]];
    [_service purchase:productId quantity:quantity.integerValue completion:^(NSError *error) {
        CDVPluginResult * result;
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self errorToDic:error]];
        }
        else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

-(void) consume:(CDVInvokedUrlCommand *) command
{
    NSString * productId = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    NSNumber * quantity = [command argumentAtIndex:1 withDefault:[NSNumber numberWithInt:1] andClass:[NSNumber class]];
    NSInteger consumed = [_service consume:productId quantity:quantity.integerValue];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:(int)consumed] callbackId:command.callbackId];
}

-(void) finishPurchase:(CDVInvokedUrlCommand *) command
{
     NSString * transactionId = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    [_service finishPurchase:transactionId];
}

-(void) setValidationHandler:(CDVInvokedUrlCommand *) command
{
    NSMutableDictionary * validationCompletions = _validationCompletions;
    id<CDVCommandDelegate> commandDelegate = self.commandDelegate;
    static NSInteger validationIndex = 0;
    [_service setValidationHandler:^(NSData *validationReceipt, NSString *productId, LDValidationCompletion completion) {
        
        NSInteger completionId = validationIndex++;
        [validationCompletions setObject:completion forKey:[NSNumber numberWithInteger:completionId].stringValue];
        NSString * strReceipt = [[NSString alloc] initWithData:validationReceipt encoding:NSUTF8StringEncoding];
        NSArray * data = @[strReceipt, productId, [NSNumber numberWithInt:(int)completionId]];
        CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:data];
        [result setKeepCallbackAsBool:YES];
        [commandDelegate sendPluginResult: result callbackId:command.callbackId];
    }];
}

-(void) validationCompletion:(CDVInvokedUrlCommand *) command
{
    NSNumber * completionId = [command argumentAtIndex:0 withDefault:[NSNumber numberWithInteger:-1] andClass:[NSNumber class]];
    NSNumber * validationResult = [command argumentAtIndex:1 withDefault:[NSNumber numberWithBool:NO] andClass:[NSNumber class]];
    LDValidationCompletion completion = [_validationCompletions objectForKey:completionId.stringValue];
    if (completion) {
        NSError * error = nil;
        if (!validationResult.boolValue) {
            error = [[NSError alloc] initWithDomain:@"LDInAppService" code:0 userInfo:@{NSLocalizedDescriptionKey:@"Custom validation rejected purchase"}];
        }
        completion(error);
        [_validationCompletions removeObjectForKey:completionId.stringValue];
    }
}

-(void) setLudeiServerValidationHandler:(CDVInvokedUrlCommand *) command
{
    [_service setLudeiServerValidationHandler];
}


#pragma mark LDInAppPurchaseObserver

-(void) inAppService:(LDInAppService *) service didStartPurchase:(NSString *) productId
{
    NSArray * args = @[@"start", productId];
    CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:args];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult: result callbackId:_listenerCallbackId];
}

-(void) inAppService:(LDInAppService *) service didFailPurchase:(NSString *) productId withError:(NSError *) error
{
    NSArray * args = @[@"error", productId, [self errorToDic:error]];
    CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:args];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult: result callbackId:_listenerCallbackId];
}

-(void) inAppService:(LDInAppService *) service didCompletePurchase:(LDInAppPurchase *) purchase
{
    NSInteger stock = [_service stockOfProduct:purchase.productId];
    NSArray * args = @[@"complete", [purchase toDictionary], [NSNumber numberWithInteger:stock]];
    CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:args];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult: result callbackId:_listenerCallbackId];
}

#pragma mark Utils

-(NSDictionary *) errorToDic:(NSError * ) error
{
    return @{@"code":[NSNumber numberWithInteger:error.code], @"message":error.localizedDescription};
}

-(NSArray *) productsToJSON:(NSArray *) products
{
    NSMutableArray * result = [NSMutableArray array];
    for (LDInAppProduct * product in products) {
        NSMutableDictionary * dic = [NSMutableDictionary dictionaryWithDictionary:[product toDictionary]];
        [dic setObject:[NSNumber numberWithInt:(int)[_service stockOfProduct:product.productId]] forKey:@"stock"];
        [result addObject:dic];
    }
    return result;
}

@end
