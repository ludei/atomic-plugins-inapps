//
//  AppDelegate.m
//  InAppTest
//
//  Created by Imanol Fernandez Gorostizaga on 2/12/14.
//  Copyright (c) 2014 Ludei. All rights reserved.
//

#import "AppDelegate.h"
#import "LDInAppService.h"
#import "TestViewController.h"
#import "TestData.h"

@interface AppDelegate () <LDInAppPurchaseObserver>

@end

@implementation AppDelegate
{
    MainTests * _mainTests;
    LDInAppService * _service;
}


-(void) inAppService:(LDInAppService *) service didStartPurchase:(NSString *) productId
{
    NSLog(@"didStartPurchase: %@", productId);
}

-(void) inAppService:(LDInAppService *) service didFailPurchase:(NSString *) productId withError:(NSError *) error
{
    NSLog(@"didFailPurchase: %@ (%@)", productId, error.localizedDescription);
}
-(void) inAppService:(LDInAppService *) service didCompletePurchase:(LDInAppPurchase *) purchase
{
    NSLog(@"didCompletePurchase: {productId: %@, transactionId: %@, quantity %d", purchase.productId, purchase.transactionId, purchase.quantity);
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    
    _service = [[LDInAppService alloc] init];
    [_service addPurchaseObserver:self];
    [_service start];
                
    _mainTests = [[MainTests alloc] init];
    _mainTests.service = _service;
    _mainTests.productIds = @[
                              @"com.ludei.basketgunner.adremoval",
                              @"com.ludei.basketgunner.getpackx1",
                              @"com.ludei.basketgunner.getpackx2",
                              @"com.ludei.basketgunner.getpackx5",
                              @"com.ludei.basketgunner.getpackx20",
                              @"com.ludei.basketgunner.getpackx50",
                              ];
    
    TestViewController *vc = [[TestViewController alloc] init];
    vc.tests = _mainTests.tests;
    
    
    UINavigationController * navigation = [[UINavigationController alloc] initWithRootViewController:vc];
    navigation.navigationBar.translucent = NO;
    
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    self.window.backgroundColor = [UIColor whiteColor];
    self.window.rootViewController = navigation;
    
    [self.window makeKeyAndVisible];
    
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

@end
