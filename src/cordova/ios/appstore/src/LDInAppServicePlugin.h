//
//  LDInAppServicePlugin.h
//  HelloCordova
//
//  Created by Imanol Fernandez Gorostizag on 12/12/14.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "LDInAppService.h"

@interface LDInAppServicePlugin : CDVPlugin<LDInAppPurchaseObserver>

@end
