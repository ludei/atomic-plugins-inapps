package com.ludei.inapptest;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ludei.inapps.InAppPurchase;
import com.ludei.inapps.InAppService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity {

    InAppService service;
    MainTests mainTests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        service = InAppServiceCreator.create(this);
        service.addPurchaseObserver(new InAppService.InAppPurchaseObserver() {
            @Override
            public void onPurchaseStart(InAppService sender, String productId) {
                Log.i("InAppService", "onPurchaseStart " + productId);
            }

            @Override
            public void onPurchaseFail(InAppService sender, String productId, InAppService.Error error) {
                Log.i("InAppService", "onPurchaseFail " + productId + "Error: " + error.message);
            }

            @Override
            public void onPurchaseComplete(InAppService sender, InAppPurchase purchase) {
                Log.i("InAppService", "onPurchaseComplete " + purchase.productId + " " + purchase.transactionId);
            }
        });
        String[] productIds = {
                "com.ludei.basketgunner.getpackx50",
                "com.ludei.basketgunner.getpackx5",
                "com.ludei.basketgunner.getpackx1",
                "com.ludei.basketgunner.getpackx2",
                "com.ludei.basketgunner.getpackx20",
                "com.ludei.basketgunner.adremoval",
        };
        mainTests = new MainTests(new ArrayList<String>(Arrays.asList(productIds)), service);

        TestFragment fragment = new TestFragment();
        fragment.setTests(mainTests.tests());
        getFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        boolean handled = service.onActivityResult(requestCode, resultCode, data);
        if (!handled) {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onDestroy()
    {
        service.onDestroy();
        super.onDestroy();
    }

    public static class TestListAdapter extends ArrayAdapter<TestData> {

        public TestListAdapter(Context ctx, int resource, List<TestData> tests) {
            super(ctx,0, tests);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.test_list_item, parent, false);
            }

            TextView txtTitle = (TextView) rowView.findViewById(R.id.txtTitle);
            TextView txtDetail = (TextView) rowView.findViewById(R.id.txtDetail);
            TextView txtRight = (TextView) rowView.findViewById(R.id.txtRight);

            TestData data = this.getItem(position);

            txtTitle.setText(data.title!= null ? data.title : "");
            txtDetail.setText(data.leftDetail!= null ? data.leftDetail : "");
            txtRight.setText(data.rightDetailt!= null ? data.rightDetailt : "");

            return rowView;
        }
    }

    public static class TestFragment extends ListFragment
    {

        private ArrayList<TestData> tests;
        private TestFragment popFragment;

        public TestFragment() {
            tests = new ArrayList<TestData>();
            tests.add(new TestData("Loading...", null));
        }

        public void setTests(List<TestData> data){
            this.tests.clear();
            if (data != null) {
                this.tests.addAll(data);
            }

            ListAdapter adapter = this.getListAdapter();
            if (adapter != null) {
                ((ArrayAdapter)adapter).notifyDataSetChanged();
            }
        }

        public void setPopFragment(TestFragment fragment) {
            this.popFragment = fragment;
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {

            TestData data = tests.get(position);
            if (data.action == null) {
                return;
            }

            if (data.isSetting) {
                if (data.action != null) {
                    data.action.run(new TestData.TestCompletion() {
                        @Override
                        public void completion(List<TestData> next, InAppService.Error error) {

                        }
                    });
                }
                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                return;
            }

            final TestFragment fragment = new TestFragment();
            fragment.setPopFragment(this);
            getFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).addToBackStack("tests").commit();

            data.action.run(new TestData.TestCompletion() {
                @Override
                public void completion(final List<TestData> next, final InAppService.Error error) {
                    if (error != null) {
                        String message = "Error " + error.message + " (code: " + error.code + ")";
                        TestData data = new TestData(message, null);
                        ArrayList<TestData> list = new ArrayList<TestData>();
                        list.add(data);
                        fragment.setTests(list);
                    }
                    else {
                        fragment.setTests(next);
                    }
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setListAdapter(new TestListAdapter(inflater.getContext(), R.layout.test_list_item, tests));
            View view = super.onCreateView(inflater, container, savedInstanceState);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
            view.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if( keyCode == KeyEvent.KEYCODE_BACK ) {
                        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        return true;
                    } else {
                        return false;
                    }
                }
            });


            return view;
        }
    }
}
