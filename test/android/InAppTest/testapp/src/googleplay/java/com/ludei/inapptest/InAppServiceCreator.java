package com.ludei.inapptest;

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
import com.ludei.inapps.googleplay.GooglePlayInAppService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class InAppServiceCreator {

    public static InAppService create(Context ctx) {
        return new GooglePlayInAppService(ctx);
    }
}
