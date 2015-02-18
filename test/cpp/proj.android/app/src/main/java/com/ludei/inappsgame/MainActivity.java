package com.ludei.inappsgame;
import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxGLSurfaceView;

import android.content.Intent;
import android.os.Bundle;

import com.safejni.SafeJNI;

public class MainActivity extends Cocos2dxActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        SafeJNI.INSTANCE.setActivity(this); //set the activity for atomic plugins and load safejni.so
        super.onCreate(savedInstanceState);
    }

    @Override
    public Cocos2dxGLSurfaceView onCreateView() {
        final Cocos2dxGLSurfaceView surfaceView = super.onCreateView();
        SafeJNI.INSTANCE.setJavaToNativeDispatcher(new SafeJNI.JavaToNativeDispatcher() {
            @Override
            public void dispatch(Runnable runnable) {
                surfaceView.queueEvent(runnable);
            }
        });

        return surfaceView;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SafeJNI.INSTANCE.onActivityResult(requestCode, resultCode, data);
    }





}
