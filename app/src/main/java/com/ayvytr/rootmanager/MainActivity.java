package com.ayvytr.rootmanager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayvytr.easyandroid.Easy;
import com.ayvytr.easyandroid.tools.withcontext.ToastTool;
import com.ayvytr.logger.L;
import com.ayvytr.root.Roots;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init()
    {
        initLibs();
        rootOperations();
    }

    private void rootOperations()
    {
        boolean b = Roots.get().hasRooted();
        L.e("Root ", b);
        ToastTool.show("Root:" + b );

        Roots.THIS.requestRootPermission();
    }

    private void initLibs()
    {
        Easy.getDefault().init(this);
        L.getSettings().justShowMessage(true);
    }
}
