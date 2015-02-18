//
//  ViewController.m
//  InAppTest
//
//  Created by Imanol Fernandez Gorostizag on 2/12/14.
//  Copyright (c) 2014 Ludei. All rights reserved.
//

#import "TestViewController.h"
#import "LDInAppService.h"
#import "TestData.h"

@interface TestViewController () <UITableViewDataSource, UITableViewDelegate>

@end

@implementation TestViewController
{
    UIRefreshControl * _refreshControl;
    UITableView * _tableView;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _tableView = [[UITableView alloc] initWithFrame:self.view.bounds];
    _tableView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    _tableView.dataSource = self;
    _tableView.delegate = self;
    _refreshControl = [[UIRefreshControl alloc] init];
    [_tableView addSubview:_refreshControl];
    [self.view addSubview:_tableView];
    
    if (!_tests) {
        [_refreshControl beginRefreshing];
    }
}

-(void) setTests:(NSArray *)tests
{
    _tests = tests;
    if (_refreshControl) {
        [_refreshControl endRefreshing];
    }
    if (_tableView) {
        [_tableView reloadData];
    }
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}



- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return _tests.count;
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString * reuseId = @"test_cell";
    
    UITableViewCell * cell = [tableView dequeueReusableCellWithIdentifier:reuseId];
    if (!cell) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:reuseId];
    }
    TestData * data = [_tests objectAtIndex:indexPath.row];
    
    cell.textLabel.text = data.name;
    cell.detailTextLabel.text = data.leftDetail ?: @"";
    
    cell.accessoryType = data.action && !data.isSetting ? UITableViewCellAccessoryDisclosureIndicator : UITableViewCellAccessoryNone;
    if (data.rightDetail) {
        UILabel * label = [[UILabel alloc] initWithFrame:CGRectZero];
        label.text = data.rightDetail;
        [label sizeToFit];
        cell.accessoryView = label;
    }
    else {
        cell.accessoryView = nil;
    }
    
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    TestData * data = [_tests objectAtIndex:indexPath.row];
    if (!data.action) {
        return;
    }
    

    if (data.isSetting) {
        data.action(^(NSArray * nextTests, NSError * error){
            
        });
        [self.navigationController popViewControllerAnimated:YES];
    }
    else {
        
        TestViewController * vc = [[TestViewController alloc] init];
        [self.navigationController pushViewController:vc animated:YES];
        data.action(^(NSArray * nextTests, NSError * error){
            
            if (error) {
                TestData * data = [[TestData alloc] initWithName:[NSString stringWithFormat:@"Error (code %ld)", (long)error.code] action:nil];
                data.leftDetail = error.localizedDescription;
                vc.tests = @[data];
            }
            else {
                vc.tests = nextTests;
            }
            
        });
        
    }


}

@end
