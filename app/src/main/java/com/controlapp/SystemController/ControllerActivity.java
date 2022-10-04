package com.controlapp.SystemController;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.control.ControlImpl;
import com.control.IControl;

import com.softwinner.Gpio;

import java.util.Timer;
import java.util.TimerTask;

public class ControllerActivity extends AppCompatActivity implements OnClickListener {

    private IControl CR = null;
    private TextView txtMsg = null;
    private int iGpioStatus = 0;
    private Timer timer =null;
    private Runnable updateTextRunnable;

    public final static int MSG_STATUS_CHANGE = 1;
    public final static int MSG_UDP_INFO = 2;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATUS_CHANGE:
                case MSG_UDP_INFO:
                    refreshLogView((String)msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        ((ControlApp)getApplication()).setHandler(mHandler);

        txtMsg = findViewById(R.id.textView_info);
        findViewById(R.id.btn0).setOnClickListener(this);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
        findViewById(R.id.btn5).setOnClickListener(this);
        findViewById(R.id.btn6).setOnClickListener(this);
        findViewById(R.id.btn7).setOnClickListener(this);
        txtMsg.setMovementMethod(ScrollingMovementMethod.getInstance());

        CR = ControlImpl.getInstance(_getConfig());

        if(false) {
            InitGPIO();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn0:
                txtMsg.setText("");
                txtMsg.scrollTo(0, 0);
                break;
            case R.id.btn1:
                CR.getGrade(IControl.GRADE_TYPE_0);
                CR.getGrade(IControl.GRADE_TYPE_1);
                break;
            case R.id.btn2:
                CR.TestForUDPCmd(0);
                break;
            case R.id.btn3:
                CR.TestForUDPCmd(1);
                break;
            case R.id.btn4:
                CR.TestForUDPCmd(2);
                break;
            case R.id.btn5:
                CR.TestForUDPCmd(4);
                break;
            case R.id.btn6:
                CR.TestForUDPCmd(3);
                break;
            case R.id.btn7:
                CR.TestForUDPCmd(5);
                break;
            default:
                break;
        }
    }

    private void refreshLogView(String msg) {
        String txt = msg.replace("[[","[<b><font color='#000000'>")
                        .replace("]]","</b><font>]")
                        .replace("[ ","[<b><font color='#0000FF'>")
                        .replace(" ]","</b><font>]")
                        .replace("< ","[<b><font color='#FF0000'>")
                        .replace(" >","</b><font>]");
        txtMsg.append(Html.fromHtml(txt));
        txtMsg.append("\n");
        //txtMsg.append(msg + "\n");
        //int offset = txtMsg.getLineCount() * txtMsg.getLineHeight();
        int offset = getTextViewHeight(txtMsg);
        if (offset > txtMsg.getHeight()) {
            txtMsg.scrollTo(0, offset - txtMsg.getHeight());
        }
    }

    private int getTextViewHeight(TextView view) {
        Layout layout = view.getLayout();
        if(null == layout) {
            return view.getLineCount() * view.getLineHeight();
        }

        int desired = layout.getLineTop(view.getLineCount());
        int padding = view.getCompoundPaddingTop() + view.getCompoundPaddingBottom();
        return desired + padding;
    }

    private void InitGPIO(){
        Gpio.setMulSel('O', 1, 0);   //初始化引脚为输入模式
        //Gpio.setPull('O',1,1);   //pull up, GND
        iGpioStatus = Gpio.readGpio('O', 1);
        refreshLogView("GPIO[O1]:" + iGpioStatus);

        refreshLogView("初始化GPIO监听线程...");
        if (timer == null) {           //线程只有一个任务，实时读取引脚状态
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int iNowStatus = Gpio.readGpio('O', 1);
                    if (iNowStatus != iGpioStatus) {
                        iGpioStatus = iNowStatus;
                        mHandler.post(updateTextRunnable);
                        if (iNowStatus == -1) {
                            timer.cancel();
                        }
                    }
                }
            }, 100, 200);//0.1s后执行。并每0.1s执行一次
        }
        refreshLogView(txtMsg.getText() + "初始化GPIO监听线程完成");

        //mHandler = new Handler();
        updateTextRunnable = new Runnable() {
            @Override
            public void run() {
                //如果不想设置监听也可让其不断地运行，保证内容的刷新，不过很浪费性能；
                //mHandler.post(this);
                //notifyDataSetChanged();
                refreshLogView( "检测到GPIO[O1]改变:[ " + iGpioStatus + " ]");
            }
        };
    }

    private byte _getConfig(){
        SharedPreferences settings = this.getSharedPreferences(IControl.CFG_FILE_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        refreshLogView(IControl.CFG_ITEM_HOSTID + ": [ " + settings.getInt(IControl.CFG_ITEM_HOSTID, 0) + " ]");
        return (byte)settings.getInt(IControl.CFG_ITEM_HOSTID, 0);
    }
}