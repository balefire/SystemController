package com.control;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.controlapp.SystemController.ControlApp;
import com.controlapp.SystemController.ControllerActivity;
import com.wits.serialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.security.InvalidParameterException;
import java.util.Arrays;

public final class ControlImpl implements IControl {

    private static IControl instance = null;
    private static UDPControl UC = null;
    private GradeData GD = GradeData.getInstance();
    private Handler mhandler = null;

    private static SerialPort mSerialPort;
    private static OutputStream mOutputStream;
    private static InputStream mInputStream;
    private final String TAG = "[FJY CTRL]";
    private final int iRevLen = 4;                //REV CMD Length: 4 Bytes 0xFFxxxxFF
    private final int iCodeByteStatus = 1;        //REV CMD Status
    private final int iCodeByteError  = 2;        //REV CMD Status

    private final int STATUS_WAIT        = 0;   //Wait
    private final int STATUS_RISE        = 1;   //Rise up
    private final int STATUS_RISE_PAUSE  = 2;   //Pause on rise, NOT used now.
    private final int STATUS_TOP         = 3;   //Top
    private final int STATUS_FALL        = 4;   //Fall down
    private final int STATUS_FALL_PAUSE  = 5;   //Pause on fall
    private final int STATUS_BOTTOM      = 6;   //Bottom

    private final String[] strStatusInfo = new String[]{    "[ WAIT ]",
                                                            "[ RISE ]",
                                                            "[ RISE_PAUSE ]",
                                                            "[ TOP ]",
                                                            "[ FALL ]",
                                                            "[ FALL_PAUSE ]",
                                                            "[ BOTTOM ]"};

    private int iStatus = STATUS_WAIT;

    public static synchronized IControl getInstance(byte bId){
        if(null == instance) {
            UC = UDPControl.getInstance(bId);
            instance = new ControlImpl();
        }

        return instance;
    }

    private ControlImpl(){
        //串口读写数据流
        try {
            mSerialPort = getSerialPort();
            if(null != mSerialPort) {
                mOutputStream = mSerialPort.getOutputStream();
                mInputStream = mSerialPort.getInputStream();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Init Serial ERROR: SecurityException");
        } catch (IOException e) {
            Log.e(TAG, "Init Serial ERROR: IOException");
        } catch (InvalidParameterException e) {
            Log.e(TAG, "Init Serial ERROR: InvalidParameterException");
        }

        if(iUsedMonitor){
            mhandler = ControlApp.getInstance().getHandler();
        }

        //Start Rev thread.
        _doReceive();
    }

    private void _setStatus(int iNewStatus){
        if(0 > iNewStatus || iNewStatus > strStatusInfo.length){
            Log.e(TAG, "[STATUS CHANGE] Status code ERROR: [ " + iNewStatus + " ]");
            return;
        }

        Log.v(TAG, "[STATUS CHANGE] " + strStatusInfo[iStatus] + " -> " + strStatusInfo[iNewStatus]);
        _notifyUI("[[STATUS CHANGE]] " + strStatusInfo[iStatus] + " -> " + strStatusInfo[iNewStatus]);

        iStatus = iNewStatus;

        return;
    }

    private void _notifyUI(String info){
        if(mhandler != null){
            Message m = mhandler.obtainMessage();
            m.what = ControllerActivity.MSG_STATUS_CHANGE;
            m.obj = info;
            m.sendToTarget();
        }

        return;
    }

    private int _getStatus(){
        return iStatus;
    }

    private void _doStatusAction(int iNewStatus) {
        if(0 > iNewStatus || iNewStatus > strStatusInfo.length){
            Log.e(TAG, "[DO-ACTION] Status code ERROR: [ " + iNewStatus + " ]");
            return;
        }

        switch(iNewStatus) {
            case STATUS_WAIT: { //Wait
                UC.Send(UDPControl.UDP_CMD_WAIT);
                _notifyUI("[[SET-ANM]] Code [ " + UDPControl.UDP_CMD_WAIT+ " ]");
                break;
            }
            case STATUS_RISE: { //Rise
                UC.Send(UDPControl.UDP_CMD_START_RAISE);
                _notifyUI("[[SET-ANM]] Code [ " + UDPControl.UDP_CMD_START_RAISE+ " ]");
                break;
            }
            case STATUS_TOP: { //Rise finish, Top
                UC.Send(UDPControl.UDP_CMD_TOP);
                _notifyUI("[[SET-ANM]] Code [ " + UDPControl.UDP_CMD_TOP+ " ]");
                break;
            }
            case STATUS_FALL_PAUSE: { //Fall pause
                UC.Send(UDPControl.UDP_CMD_STOP_FALL);
                _notifyUI("[[SET-ANM]] Code [ " + UDPControl.UDP_CMD_STOP_FALL+ " ]");
                break;
            }
            case STATUS_FALL: { //Falling
                UC.Send(UDPControl.UDP_CMD_START_FALL);
                _notifyUI("[[SET-ANM]] Code [ " + UDPControl.UDP_CMD_START_FALL+ " ]");
                break;
            }
            case STATUS_BOTTOM: { //Fall finish, Bottom
                UC.Send(UDPControl.UDP_CMD_BOTTOM);
                _notifyUI("[[SET-ANM]] Code [ " + UDPControl.UDP_CMD_BOTTOM + " ]");
                break;
            }
            default: {
                Log.e(TAG, "[DO-ACTION] Unknown Code:< " + iNewStatus + " >");
                break;
            }
        }
    }

    private byte[] getCMDResultData() throws IOException {
        return getCMDResultData(iRevLen);
    }

    private byte[] getCMDResultData(int len) throws IOException {

        byte[] buffer = new byte[iRevLen];
        byte[] result = new byte[iRevLen];
        int size = 0;
        int count = 0;

        if(0 >= len || iRevLen < len) {
            Log.e(TAG, "REV LEN ERROR: [ " + len + " ] !!!");
            return null;
        }
        if (mInputStream == null) {
            Log.e(TAG, "REV mInputStream == NULL !!!");
            return null;
        }

        Arrays.fill(result, (byte) 0);

        while (count < len) {
            Arrays.fill(buffer, (byte) 0);

            Log.i(TAG, "REV LOOP START...");
            size = mInputStream.read(buffer);
            Log.i(TAG, "REV LOOP SIZE:[ " + size + " ]");

            if (size > 0) {
                Log.i(TAG, "REV COUNT[ " +count + " ] SIZE[ " + size + " ]:[ " + HexConverUtils.bytesToHex(buffer, 0, size) + " ]");
                System.arraycopy(buffer,0, result, count, size);
                count += size;
            }
        }

        if(count != len)
        {
            Log.e(TAG, "REV ERROR: COUNT:< " + count + " > LEN:< " + len + " > !!!");
            return null;
        }
        return result;
    }

    //打开串口
    private SerialPort getSerialPort() throws SecurityException, IOException,
            InvalidParameterException {
        Log.i(TAG, "Open Serial Port TTY-Name: [ " + sAddress + " ] Baud-Rate:[ " + iBaudRate + " ]");
        File file = new File(sAddress);
        Log.i(TAG, "Open Serial file: [ " + file.toString() + " ]");

        if(!file.canRead() || !file.canWrite()) {
            Log.e(TAG, "Open Serial file FAIL !!!");
        }

        mSerialPort = new SerialPort(file, iBaudRate, 0);
        return mSerialPort;
    }

    //关闭串口
    private void closeSerialPort() {
        Log.i(TAG, "Close Serial Port.");
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
            if (mOutputStream != null) {
                mOutputStream.close();
            }
            if (mSerialPort != null) {
                mSerialPort.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getGrade(int type)
    {
        if(type < GRADE_TYPE_MIN || type > GRADE_TYPE_MAX)
        {
            Log.e(TAG, "Grade-Type ERROR !!! < " + type + " >");
            return null;
        }
        String ret = "";
        byte[] data = GD.GetGrade(type);
        if(null != data)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                sb.append("" + data[i]);
            }
            Log.v(TAG, "getGrade [ " + sb.toString() + " ]");
            _notifyUI("[[GRADE]][ " + type + " ][ " + sb.toString() + " ]");
            return sb.toString();
        }
        else{
            Log.e(TAG, "getGrade [ null ]");
            _notifyUI("[[GRADE]][ " + type + " ][ NULL ]");
            return null;
        }
    }

    protected void finalize() throws java.lang.Throwable {
        super.finalize();
        closeSerialPort();
        mSerialPort = null;
    }

    private void _doReceive() {
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "REV thread START...");
                byte[] mBuffer = null;
                while (true) {
                    try {
                        mBuffer = getCMDResultData();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(null == mBuffer) {
                        Log.e(TAG, "[_doReceive] getCMDResultData ERROR !!! ");
                        //continue;
                        return; //To be fix!!!
                    }

                    Log.v(TAG, "SERIAL GET:[ " + mBuffer.length + " ][ " + HexConverUtils.bytesToHex(mBuffer) + " ]");

                    //Deal Error CMD
                    switch(mBuffer[iCodeByteError]) {
                        case 0: { //Normal, continue to deal STATUS cmd
                            break;
                        }
                        case 1: //Time-out on rise
                        case 2: //Time-out on fall
                        case 3: //Error for limit-switch
                        default: {
                            Log.v(TAG, "[STATUS] Err-Code:< " + HexConverUtils.bytesToHex(mBuffer) + " >");
                            _notifyUI("[[STATUS]] Err-Code:< " + HexConverUtils.bytesToHex(mBuffer) + " >");
                            if(STATUS_WAIT != _getStatus()) {
                                _setStatus(STATUS_WAIT);
                                {
                                    //need to add for UDP
                                }
                            }
                            continue;
                        }
                    }

                    //Deal Status CMD
                    if(!_doStatusCMD(mBuffer[iCodeByteStatus])) {
                        Log.e(TAG, "[STATUS] Unknown Status Code:< " + HexConverUtils.bytesToHex(mBuffer) + " >");
                        _notifyUI("[[STATUS]] Unknown Status Code:< " + HexConverUtils.bytesToHex(mBuffer) + " >");
                    }
                }
            }
        }).start();
    }

    private boolean _doStatusCMD(int iCMD) {
        switch(iCMD) {
            case 0: { //Wait
                if (STATUS_WAIT != _getStatus()) {
                    _setStatus(STATUS_WAIT);
                    _doStatusAction(STATUS_WAIT);
                }
                break;
            }
            case 1: { //Rise
                if (STATUS_RISE != _getStatus()) {
                    _setStatus(STATUS_RISE);
                    _doStatusAction(STATUS_RISE);
                }
                break;
            }
            case 2: { //Rise finish, Top
                if (STATUS_TOP != _getStatus()) {
                    _setStatus(STATUS_TOP);
                    _doStatusAction(STATUS_TOP);
                }
                break;
            }
            case 3: { //Fall pause
                if (STATUS_FALL_PAUSE != _getStatus()) {
                    _setStatus(STATUS_FALL_PAUSE);
                    _doStatusAction(STATUS_FALL_PAUSE);
                }
                break;
            }
            case 4: { //Falling
                if (STATUS_FALL != _getStatus()) {
                    _setStatus(STATUS_FALL);
                    _doStatusAction(STATUS_FALL);
                }
                break;
            }
            case 5: { //Fall finish, Bottom
                if (STATUS_BOTTOM != _getStatus()) {
                    _setStatus(STATUS_BOTTOM);
                    _doStatusAction(STATUS_BOTTOM);
                }
                break;
            }
            default: {
                return false;
            }
        }

        return true;
    }

    public void TestForUDPCmd(int iCMD) {
        _doStatusCMD(iCMD);
    }
}
