package com.ludei.inapptest;

import android.util.Log;

import com.ludei.inapps.InAppProduct;
import com.ludei.inapps.InAppPurchase;
import com.ludei.inapps.InAppService;

import java.util.ArrayList;
import java.util.List;

public class MainTests {

    private ArrayList<String> productIds;
    private InAppService service;

    public MainTests(ArrayList<String> productIds, InAppService service) {
        this.productIds = productIds;
        this.service = service;
    }


    private TestData purchaseTests(final InAppProduct product) {

        TestData test = new TestData(product.title, new TestData.TestAction() {
            @Override
            public void run(final TestData.TestCompletion completion) {
                service.purchase(product.productId, new InAppService.PurchaseCallback() {
                    @Override
                    public void onComplete(InAppPurchase purchase, InAppService.Error error) {
                        ArrayList<TestData> next = new ArrayList<TestData>();
                        if (error == null) {
                            next.add(new TestData("Succesfully purchased", null));
                        }
                        completion.completion(next, error);
                    }
                });
            }
        });

        test.leftDetail = "Click to purchase";
        return test;
    }

    private TestData consumeTests(final InAppProduct product) {

        TestData test = new TestData(product.title, new TestData.TestAction() {
            @Override
            public void run(final TestData.TestCompletion completion) {
                service.consume(product.productId, 1, new InAppService.ConsumeCallback() {
                    @Override
                    public void onComplete(int consumed, InAppService.Error error) {
                        ArrayList<TestData> next = new ArrayList<TestData>();
                        if (error == null) {
                            next.add(new TestData("Consumed items: " + consumed, null));
                        }
                        completion.completion(next, error);
                    }
                });
            }
        });

        test.leftDetail = "Click to consume (stock: " + service.stockOfProduct(product.productId) + ")";
        return test;
    }

    public ArrayList<TestData> tests()
    {
        ArrayList<TestData> result = new ArrayList<TestData>();

        result.add(new TestData("Fetch products from store", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                service.fetchProducts(productIds, new InAppService.FetchCallback() {
                    @Override
                    public void onComplete(List<InAppProduct> products, InAppService.Error error) {

                        ArrayList<TestData> next = new ArrayList<TestData>();
                        for (InAppProduct product: products) {
                            TestData test = new TestData(product.title, null);
                            test.leftDetail = product.description;
                            test.rightDetailt = product.localizedPrice;
                            next.add(test);
                        }
                        completion.completion(next, error);
                    }
                });

            }
        }));

        result.add(new TestData("List cached products", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                List<InAppProduct> products = service.getProducts();
                ArrayList<TestData> next = new ArrayList<TestData>();
                for (InAppProduct product: products) {
                    TestData test = new TestData(product.title, null);
                    test.leftDetail = product.description;
                    test.rightDetailt = product.localizedPrice;
                    next.add(test);
                }
                completion.completion(next, null);
            }
        }));

        result.add(new TestData("Check purchased and stock", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                List<InAppProduct> products = service.getProducts();
                ArrayList<TestData> next = new ArrayList<TestData>();
                for (InAppProduct product: products) {
                    TestData test = new TestData(product.title, null);
                    boolean purchased = service.isPurchased(product.productId);
                    int stock = service.stockOfProduct(product.productId);
                    test.leftDetail = "Purchased: " + purchased + " stock: " + stock;
                    test.rightDetailt = product.localizedPrice;
                    next.add(test);
                }
                completion.completion(next, null);
            }
        }));

        result.add(new TestData("Purchase product", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                List<InAppProduct> products = service.getProducts();
                ArrayList<TestData> next = new ArrayList<TestData>();
                for (InAppProduct product: products) {
                    next.add(purchaseTests(product));
                }
                completion.completion(next, null);
            }
        }));

        result.add(new TestData("Consume product", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                List<InAppProduct> products = service.getProducts();
                ArrayList<TestData> next = new ArrayList<TestData>();
                for (InAppProduct product: products) {
                    next.add(consumeTests(product));
                }
                completion.completion(next, null);
            }
        }));

        result.add(new TestData("Restore products", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                service.restorePurchases(new InAppService.RestoreCallback() {
                    @Override
                    public void onComplete(InAppService.Error error) {
                        ArrayList<TestData> next = new ArrayList<TestData>();
                        if (error == null) {
                            next.add(new TestData("Restore Succeeded", null));
                        }
                        completion.completion(next, error);
                    }
                });
            }
        }));

        result.add(new TestData("Set validation mode", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                ArrayList<TestData> next = new ArrayList<TestData>();

                next.add(new TestData("No validation", new TestData.TestAction() {
                    @Override
                    public void run(TestData.TestCompletion completion) {
                        service.setValidationHandler(null);
                        completion.completion(null,null);

                    }
                }));

                next.add(new TestData("Custom validation", new TestData.TestAction() {
                    @Override
                    public void run(TestData.TestCompletion completion) {
                        service.setValidationHandler(new InAppService.ValidationHandler() {
                            @Override
                            public void onValidate(String receipt, String productId, InAppService.ValidationCompletion completion) {
                                Log.i("InAppService", "Custom validation: " + receipt + " productId:" + productId);
                                completion.finishValidation(null);
                            }
                        });
                        completion.completion(null,null);

                    }
                }));

                next.add(new TestData("Ludei server validation", new TestData.TestAction() {
                    @Override
                    public void run(TestData.TestCompletion completion) {
                        service.setLudeiServerValidationHandler();
                        completion.completion(null,null);

                    }
                }));

                for (TestData test: next) {
                    test.isSetting = true;
                }
                completion.completion(next, null);
            }
        }));

        result.add(new TestData("Can purchase?", new TestData.TestAction(){
            public void run(final TestData.TestCompletion completion) {

                boolean can = service.canPurchase();
                ArrayList<TestData> next = new ArrayList<TestData>();
                next.add(new TestData("Result: " + can, null));
                completion.completion(next, null);
            }
        }));

        return result;

    }

}
