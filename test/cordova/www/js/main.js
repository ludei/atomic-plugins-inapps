
var mainTests;
var service;
var productIds = [
    "com.ludei.basketgunner.adremoval",
    "com.ludei.basketgunner.getpackx1",
    "com.ludei.basketgunner.getpackx2",
    "com.ludei.basketgunner.getpackx5",
    "com.ludei.basketgunner.getpackx20",
    "com.ludei.basketgunner.getpackx50",
];

var navigation = [];


function TestData(name, action) {
    this.name = name;
    this.leftDetail = null;
    this.rightDetail = null;
    this.action = action || null;
    this.setting = false;
}


function createConsumeTest(product) {

    var test = new TestData(product.title, function(completion) {
        service.consume(product.productId, 1, function(consumed, error) {
            var result = new TestData("Consumed items: " + consumed);
            completion([result], error);
        });
    });
    test.leftDetail = "Click to consume (stock: " + service.stockOfProduct(product.productId) + ")" ;
    return test;
}

function createPurchaseTest(product) {

    var test = new TestData(product.title, function(completion) {
        service.purchase(product.productId, 1, function(error) {
            var result = new TestData("Successfully purchased");
            completion([result], error);
        });
    });
    test.leftDetail = "Click to purchase";
    return test;
}

function createMainTests() {
    return [
        new TestData("Fetch products from store", function(completion){
            service.fetchProducts(productIds, function(products, error){
                var next = [];
                for (var i = 0; i < products.length; ++i) {
                    var product = products[i];
                    var test = new TestData(product.title);
                    test.leftDetail = product.description;
                    test.rightDetail = product.localizedPrice;
                    next.push(test);
                }
                completion(next, error);
            });
        }),
        new TestData("List cached products", function(completion){
            var next = [];
            var products = service.getProducts();
            for (var i = 0; i < products.length; ++i) {
                var product = products[i];
                var test = new TestData(product.title);
                test.leftDetail = product.description;
                test.rightDetail = product.localizedPrice;
                next.push(test);
            }
            completion(next, null);
        }),
        new TestData("Check purchases and stock", function(completion){
            var next = [];
            var products = service.getProducts();
            for (var i = 0; i < products.length; ++i) {
                var product = products[i];
                var test = new TestData(product.title);
                var purchased = service.isPurchased(product.productId);
                var stock = service.stockOfProduct(product.productId);
                test.leftDetail = "purchased: " + purchased + " stock: " + stock;
                test.rightDetail = product.localizedPrice;
                next.push(test);
            }
            completion(next, null);
        }),
        new TestData("Purchase product", function(completion){
            var next = [];
            var products = service.getProducts();
            for (var i = 0; i < products.length; ++i) {
                next.push(createPurchaseTest(products[i]));
            }
            completion(next, null);
        }),
        new TestData("Consume product", function(completion){
            var next = [];
            var products = service.getProducts();
            for (var i = 0; i < products.length; ++i) {
                next.push(createConsumeTest(products[i]));
            }
            completion(next, null);
        }),
        new TestData("Restore purchases", function(completion){
            service.restorePurchases(function(error){
                completion([new TestData("Purchases restored")], error);
            });
        }),
        new TestData("Set validation mode", function(completion){
            var next = [
                new TestData("No validation", function(completion){
                    service.setValidationHandler(null);
                    completion();
                }),
                new TestData("Custom validation", function(completion){
                    service.setValidationHandler(function(receipt, productId, validationCompletion){
                        console.log("Custom validation: " + receipt + " " + productId);
                        validationCompletion(true);
                    });
                    completion();
                }),
                new TestData("Ludei server validation", function(completion){
                    service.setLudeiServerValidationHandler();
                    completion();
                })
            ];
            for (var i = 0; i < next.length; ++i) {
                next[i].setting = true;
            }
            completion(next, null);
        })
    ];
}

function createTableNode(title, leftDetail, rightDetail, clickAction) {
    var li = document.createElement("li");
    li.className = "table-view-cell media";
    var a = document.createElement("a");
    a.href = "#";
    if (clickAction) {
        a.className = "navigate-right";
        a.setAttribute("data-transition", "slide-in");
        a.onclick = clickAction;
    }
    else {
        a.onclick = function() {
            return false;
        }
    }

    title = title || "";
    leftDetail = leftDetail || "";
    rightDetail = rightDetail || "";

    var html =  "<div class='pull-left'>" +
    title + "<p>" + leftDetail + "</p></div>" +
        "<div class='pull-right'><p>" + rightDetail + "</p></div>";
    a.innerHTML = html;

    li.appendChild(a);
    return li;
}

function clickAction(test) {
    if (!test.action) {
        return null;
    }
    return function() {
        var test = this;
        if (test.setting) {
            test.action(function(){});
            onBack();
        }
        else {
            showTests([new TestData("Loading...")], null);
            test.action(function(next, error) {
                showTests(next, error);
                navigation.push({next:next, error:error});
                showBackButton(true);
            });
        }
        return false;
    }.bind(test);
}

function showTests(tests, error) {
    var container = document.getElementById("tests_ul");
    while (container.firstChild) {
        container.removeChild(container.firstChild);
    }
    if (error) {
        container.appendChild(createTableNode("Error", JSON.stringify(error), "", false));
    }
    else if (tests) {
        for (var i = 0; i < tests.length; ++i) {
            var test = tests[i];
            var node = createTableNode(test.name, test.leftDetail, test.rightDetail, clickAction(test));
            container.appendChild(node);
        }
    }
}

function showBackButton(visible) {
    document.getElementById("button_back").style.visibility = visible ? "visible" : "hidden";
}

function onBack() {
    var data = navigation.pop();
    if (navigation.length > 0) {
        var data = navigation[navigation.length - 1];
        showTests(data.next, data.error);
    }

    showBackButton(navigation.length > 1);
}

function main() {
    service = Cocoon.InApp;
    service.on("purchase", {

        start: function(productId) {
            console.log("purchase started " + productId);
        },
        error: function(productId, error) {
            console.log("purchase failed " + productId + " error: " + JSON.stringify(error));
        },
        complete: function(purchase) {
            console.log("purchase completed " + JSON.stringify(purchase));
        }
    });
    service.initialize({}, function(error) {
        mainTests = createMainTests();
        showTests(mainTests);
        navigation.push({next:mainTests, error:null});
    });
}

document.addEventListener('deviceready', main, false);






