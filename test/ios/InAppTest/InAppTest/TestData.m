//
//  TestActions.m
//  InAppTest
//
//  Created by Imanol Fernandez Gorostizag on 2/12/14.
//  Copyright (c) 2014 Ludei. All rights reserved.
//

#import "TestData.h"
#import "LDInAppService.h"

static inline TestData * MAKE_TEST(NSString * name, TestAction action) {
    return [[TestData alloc] initWithName:name action:action];
}

@implementation TestData

-(instancetype) initWithName:(NSString *) name action:(TestAction) action
{
    if (self = [super init]) {
        _action = action;
        _name = name;
    }
    return self;
}

@end


@implementation MainTests



-(TestData *) consumeTest:(LDInAppProduct *) product
{
    TestData * test = MAKE_TEST(product.localizedTitle, ^(TestCompletion completion) {
        NSInteger consumed = [_service consume:product.productId quantity:1];
        NSInteger stock = [_service stockOfProduct:product.productId];
        
        TestData * data = MAKE_TEST(product.localizedTitle, nil);
        data.leftDetail = [NSString stringWithFormat:@"Consumed: %ld, Stock: %ld", (long)consumed, (long)stock];
        data.rightDetail = product.localizedPrice;
        completion(@[data], nil);
        
    });
    test.leftDetail = [NSString stringWithFormat:@"Click to consume. Current stock: %ld", (long)[_service stockOfProduct:product.productId]];
    return test;
}


-(TestData *) purchaseTest:(LDInAppProduct *) product
{
    TestData * test = MAKE_TEST(product.localizedTitle, ^(TestCompletion completion) {
        [_service purchase:product.productId completion:^(NSError *error) {
            TestData * result = MAKE_TEST(@"Successfully purchased", nil);
            completion(@[result], error);
        }];
    });
    test.leftDetail = @"Click to purchase";
    return test;
}

-(NSArray *) tests
{
    return @[
             
    MAKE_TEST(@"Fetch Products from store", ^(TestCompletion completion) {
        
        [_service fetchProducts:_productIds completion:^(NSArray *products, NSError *error) {
            
            NSMutableArray * next = [NSMutableArray array];
            for (LDInAppProduct * product in products) {
                TestData * test = MAKE_TEST(product.localizedTitle, nil);
                test.leftDetail = product.localizedDescription;
                test.rightDetail = product.localizedPrice;
                [next addObject:test];
            }
            completion(next, error);
      
        }];
    }),
    
    MAKE_TEST(@"List cached products", ^(TestCompletion completion) {
        
        NSMutableArray * next = [NSMutableArray array];
        for (LDInAppProduct * product in _service.products) {
            TestData * test = MAKE_TEST(product.localizedTitle, nil);
            test.leftDetail = product.localizedDescription;
            test.rightDetail = product.localizedPrice;
            [next addObject:test];
        }
        completion(next, nil);

    }),
    
    MAKE_TEST(@"Check Purchased and stock", (^(TestCompletion completion) {
        
        NSMutableArray * next = [NSMutableArray array];
        for (LDInAppProduct * product in _service.products) {
            TestData * test = MAKE_TEST(product.localizedTitle, nil);
            test.rightDetail = product.localizedPrice;
            BOOL purchased = [_service isPurchased:product.productId];
            NSInteger stock = [_service stockOfProduct:product.productId];
            test.leftDetail = [NSString stringWithFormat:@"purchased: %@, stock: %ld", (purchased ? @"YES" : @"NO"), (long)stock];
            [next addObject:test];
        }
        completion(next, nil);
    })),
    
    MAKE_TEST(@"Purchase product", (^(TestCompletion completion) {
        
        NSMutableArray * next = [NSMutableArray array];
        for (LDInAppProduct * product in _service.products) {
            [next addObject:[self purchaseTest:product]];
        }
        completion(next, nil);
    })),
    
    MAKE_TEST(@"Consume product", (^(TestCompletion completion) {
        
        NSMutableArray * next = [NSMutableArray array];
        for (LDInAppProduct * product in _service.products) {
            [next addObject:[self consumeTest:product]];
        }
        completion(next, nil);
    })),
    
    MAKE_TEST(@"Restore purchases", (^(TestCompletion completion) {
        
        [_service restorePurchases:^(NSError *error) {
            completion(error ? nil : @[MAKE_TEST(@"Purchases Restored", nil)], error);
        }];
    })),
    
    MAKE_TEST(@"Set Validation Mode", (^(TestCompletion completion) {
        
        NSArray * next = @[
            MAKE_TEST(@"No validation", ^(TestCompletion completion) {
                [_service setValidationHandler:nil];
                completion(nil, nil);
            }),
            MAKE_TEST(@"Custom validation", ^(TestCompletion completion) {
                [_service setValidationHandler:^(NSData *validationReceipt, NSString *productId, LDValidationCompletion completion) {
                    
                    NSLog(@"Custom validator: %@", productId);
                    completion(nil);
                    
                }];
                completion(nil, nil);
            }),
            MAKE_TEST(@"Ludei Server validation", ^(TestCompletion completion) {
                [_service setLudeiServerValidationHandler];
                completion(nil, nil);
            }),
            
        ];
        for (TestData * test in next) {
            test.isSetting = YES;
        }
        completion(next, nil);
        
    })),
    
    ];
}

@end