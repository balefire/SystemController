package com.controlapp.SystemController;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.control.IControl;

public class SettingActivity extends AppCompatActivity {

    private Button mbtn0 = null;
    private Button mbtn1 = null;
    private TextView txtMsg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mbtn0 = (Button) findViewById(R.id.btn0);
        mbtn1 = (Button) findViewById(R.id.btn1);
        txtMsg = (TextView) findViewById(R.id.textView_info);
        txtMsg.setMovementMethod(ScrollingMovementMethod.getInstance());

        _setConfig();

        mbtn0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mbtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    private void refreshLogView(String msg) {
        txtMsg.append(msg + "\n");
        int offset = txtMsg.getLineCount() * txtMsg.getLineHeight();
        if (offset > txtMsg.getHeight()) {
            txtMsg.scrollTo(0, offset - txtMsg.getHeight());
        }
    }

    private void _setConfig(){
        SharedPreferences settings = this.getSharedPreferences(IControl.CFG_FILE_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(IControl.CFG_ITEM_HOSTID, 1);
        editor.commit();

        refreshLogView(IControl.CFG_ITEM_HOSTID + ":" + settings.getInt("HostID", 0));
    }

}