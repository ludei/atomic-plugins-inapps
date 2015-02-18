package com.ludei.inapptest;

import com.ludei.inapps.InAppService;

import java.util.List;

public class TestData {

    public String title;
    public String leftDetail;
    public String rightDetailt;
    public TestAction action;
    public boolean isSetting;

    public TestData(String title, TestAction action) {
        this.title = title;
        this.action = action;
    }

    public interface TestAction {
        void run(TestCompletion completion);
    }

    public interface TestCompletion {
        void completion(List<TestData> next, InAppService.Error error);
    }
}
