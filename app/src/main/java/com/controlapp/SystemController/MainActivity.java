package com.controlapp.SystemController;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	
	Button btController, btSetting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

    }

    private void initView(){
        btController =findViewById(R.id.btController);
        btSetting =findViewById(R.id.btSetting);

        btController.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                Bundle data=new Bundle();
                data.putInt("myinfo",9);
                intent.putExtra("data",data);
                intent.setClass(MainActivity.this, ControllerActivity.class);
                startActivity(intent);
            }
        });

        btSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                Bundle data=new Bundle();
                data.putInt("myinfo",11);
                intent.putExtra("data",data);
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
    }
}
