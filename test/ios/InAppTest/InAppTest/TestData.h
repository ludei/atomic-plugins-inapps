//
//  TestActions.h
//  InAppTest
//
//  Created by Imanol Fernandez Gorostizaga on 2/12/14.
//  Copyright (c) 2014 Ludei. All rights reserved.
//

#import <Foundation/Foundation.h>
@class LDInAppService;

typedef void (^TestCompletion)(NSArray * nextTests, NSError * error);
typedef void (^TestAction)(TestCompletion completion);

@interface TestData : NSObject

-(instancetype) initWithName:(NSString *) name action:(TestAction) action;

@property (nonatomic, strong) NSString * name;
@property (nonatomic, strong) NSString * leftDetail;
@property (nonatomic, strong) NSString * rightDetail;
@property (nonatomic, strong) TestAction action;
@property (nonatomic, assign) BOOL isSetting;

@end

@interface MainTests : NSObject

@property (nonatomic, strong) LDInAppService * service;
@property (nonatomic, strong) NSArray * productIds;

-(NSArray *) tests;

@end
