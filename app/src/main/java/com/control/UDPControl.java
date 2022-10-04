package com.control;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.controlapp.SystemController.ControlApp;
import com.controlapp.SystemController.ControllerActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

class PacketData {
    public byte[] data;
    public String sIP;

    public PacketData(byte[] ArrayData, String strIP) {
        data = ArrayData;
        sIP = strIP;
    }

    private PacketData() {
    }
}

public final class UDPControl {

    private static UDPControl instance = null;
    private Handler mHandler = null;
    private DatagramSocket socket = null;
    private Timer timer_check_client = null;
    private String sLocal_IP = null;
    private String sClient_IP = null;
    private BlockingQueue<PacketData> blockingQueue = new LinkedBlockingDeque<PacketData>();
    private GradeData gd = GradeData.getInstance();


    private final static int STATUS_NO_CLIENT = 0;
    private final static int STATUS_UDP_READY = 1;
    private int iStatus = STATUS_NO_CLIENT;

    private final static String TAG = "[FJY UDP]";
    private final int DEFAULT_HOST_PORT = 20000;
    private final int DEFAULT_CLIENT_PORT = 22000;
    private final int FIND_CLIENT_PERIOD = 20000; //ms
    private static byte iHostID = 0;

    private final int CMD_MAX_LEN = 16; //Used 13

    private final static byte UDP_CMD_FIND_CLIENT   = 0x00;
    public final static byte  UDP_CMD_START_RAISE   = 0x01;
    public final static byte  UDP_CMD_START_FALL    = 0x02;
    public final static byte  UDP_CMD_STOP_FALL     = 0x03;
    public final static byte  UDP_CMD_TOP           = 0x04;
    public final static byte  UDP_CMD_BOTTOM        = 0x05;
    public final static byte  UDP_CMD_WAIT          = 0x08;

    public static synchronized UDPControl getInstance(byte bId){
        if(null == instance) {
            iHostID = bId;
            instance = new UDPControl();
        }

        Log.v(TAG, "Get Config " + IControl.CFG_ITEM_HOSTID + " : [ " + iHostID + " ]");
        return instance;
    }

    private UDPControl() {

        if(IControl.iUsedMonitor){
            mHandler = ControlApp.getInstance().getHandler();
        }

        try {
            if (null == socket) {
                socket = new DatagramSocket(DEFAULT_HOST_PORT);
            }

            Log.v(TAG, "Host Socket IP : [ " + socket.getLocalAddress().getHostName() + " ]");

            sLocal_IP = _getIpAdd();
            Log.v(TAG, "Local IP : [ " + sLocal_IP + " ]");
            _notifyUI("Local IP : [ " + sLocal_IP + " ]");

        } catch (Exception e) {
            Log.e(TAG, "Create Socket failed!!!");
            e.printStackTrace();
        }

        _doSend();
        _doReceive();
        _findClient();
    }

    private void _notifyUI(String info){
        if(mHandler != null){
            Message m = mHandler.obtainMessage();
            m.what = ControllerActivity.MSG_UDP_INFO;
            m.obj = info;
            m.sendToTarget();
        }

        return;
    }

    private String _getIpAdd() throws SocketException, UnknownHostException{
        String ip="";
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            String name = intf.getName();
            if (!name.contains("docker") && !name.contains("lo")) {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    //获得IP
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipaddress = inetAddress.getHostAddress().toString();
                        if (!ipaddress.contains("::") && !ipaddress.contains("0:0:") && !ipaddress.contains("fe80")) {
                            Log.v(TAG, "ENUM local IP : [ " + ipaddress + " ]");
                            if(!"127.0.0.1".equals(ip)){
                                ip = ipaddress;
                            }
                        }
                    }
                }
            }
        }
        return ip;
    }

    private void _doSend() {
        new Thread(new Runnable() {
            public void run() {

                if(true){ //Test new function for get local ip.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            _notifyUI("[[TFN]] HostIP : < " + IpUtils.getLocalIp4Address().get().getHostAddress() + " >");
                            _notifyUI("[[TFN]] IP : < " + HexConverUtils.bytesToHex(IpUtils.getLocalIp4Address().get().getAddress())+ " >");
                            _notifyUI("[[TFN]] HostName : < " + IpUtils.getLocalIp4Address().get().getHostName()+ " >");
                            _notifyUI("[[TFN]] Canonical HostName : < " + IpUtils.getLocalIp4Address().get().getCanonicalHostName()+ " >");
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                    }
                }

                PacketData data;
                try {
                    while(true) {
                        data = blockingQueue.take();
                        _Send(data);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "SendQueue take() Error !!!");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void _findClient() {
        String sIPHead = sLocal_IP.substring(0 , sLocal_IP.lastIndexOf('.') + 1);
        Log.v(TAG, "Finding Client, Head = [ " + sIPHead + " ]");

        if(null != timer_check_client) {
            timer_check_client.cancel();
        }
        timer_check_client = new Timer();
        timer_check_client.schedule(new TimerTask() {
            @Override
            public void run() {
                if(STATUS_UDP_READY == iStatus) {
                    timer_check_client.cancel();
                } else {
                    byte[] data = new byte[2];
                    data[0] = UDP_CMD_FIND_CLIENT;
                    data[1] = iHostID;
                    for(int i = 1; i < 255; i++) {
                        _SendData(data, sIPHead + i);
                    }
                }
            }
        },100,FIND_CLIENT_PERIOD);
    }

    private void Send(byte[] data) {
        if(STATUS_UDP_READY != iStatus) {
            Log.e(TAG, "UDP is not ready !!!");
            return;
        }

        if(null == data || data.length <= 0) {
            Log.e(TAG, "Data format Error !!!");
            return;
        }

        _SendData(data, sClient_IP);
    }

    public void Send(byte data) {
        if(STATUS_UDP_READY != iStatus) {
            Log.e(TAG, "UDP is not ready !!!");
            return;
        }

        _SendData(data, sClient_IP);
    }

    private void _SendData(byte data, String sIP) {
        byte[] b = new byte[1];
        b[0] = data;
        try {
            blockingQueue.put(new PacketData(b,sIP));
        } catch (InterruptedException e) {
            Log.e(TAG, "SendQueue put() Error !!!");
            e.printStackTrace();
        }
    }

    private void _SendData(byte[] data, String sIP) {
        if(null == data || 0 == data.length){
            Log.e(TAG, "SEND Data Format Error !!!");
            return;
        }

        try {
            blockingQueue.put(new PacketData(data,sIP));
        } catch (InterruptedException e) {
            Log.e(TAG, "SendQueue put() Error !!!");
            e.printStackTrace();
        }
    }

    private void _Send(PacketData pd) {
        if(null == pd || null == pd.data || 0 == pd.data.length) {
            Log.e(TAG, "SEND MSG Length is 0 !!!");
            return;
        }

        try {
            DatagramPacket pack = new DatagramPacket(pd.data, pd.data.length, InetAddress.getByName(pd.sIP), DEFAULT_CLIENT_PORT); // 创建DatagramPacket 对象数据包，这里的6024是我们要发送信息主机的端口号
            socket.send(pack);
            Log.v(TAG, "SEND: [ " + pd.sIP +" ][ " + DEFAULT_CLIENT_PORT + " ][ " + HexConverUtils.bytesToHex(pd.data) + " ]");
        } catch (Exception e) {
            Log.e(TAG, "SEND MSG failed!!!");
            e.printStackTrace();
        }
    }

    private void _doReceive() {
        new Thread( new Runnable() {
            public void run() {
                Log.v(TAG, "REV thread [START]...");
                while (true) {
                    byte[] data = new byte[CMD_MAX_LEN];
                    DatagramPacket pack = new DatagramPacket(data, data.length);;
                    try {
                        socket.receive(pack);
                    } catch (Exception e) {
                        Log.e(TAG, "REV FAILED!!!");
                        e.printStackTrace();
                    }

                    Log.v(TAG, "REV:[ " + pack.getLength() + " ][ " + pack.getAddress().getHostAddress() + " ][ " + HexConverUtils.bytesToHex(pack.getData(),0,pack.getLength()) + " ]");

                    switch(pack.getData()[0]) {
                        case 0:{
                            _doCaseFindClinet(pack);
                            break;
                        }
                        case 6:{
                            gd.SetGrade(IControl.GRADE_TYPE_0, pack.getData(), 1);
                            break;
                        }
                        case 7:{
                            gd.SetGrade(IControl.GRADE_TYPE_1, pack.getData(), 1);
                            break;
                        }
                        default: {
                            Log.v(TAG, "REV Unknown: [ " + pack.getLength() + " ][ " + pack.getAddress().getHostAddress() + " ][ " + HexConverUtils.bytesToHex(pack.getData(),0,pack.getLength()) + " ]");
                            break;
                        }
                    }
                }
            }
        }).start();
    }

    private void _doCaseFindClinet(DatagramPacket dp){
        if(dp.getLength() != 2 || dp.getData()[1] != iHostID) {
            Log.v(TAG, "[UDP] FIND NO-MATCH:[ " + dp.getAddress().getHostAddress() + " ]");
            _notifyUI("[[UDP]] FIND NO-MATCH:[ " + dp.getAddress().getHostAddress() + " ]");
            return;
        }

        sClient_IP = dp.getAddress().getHostAddress();
        iStatus = STATUS_UDP_READY;
        Log.v(TAG, "[UDP] FIND CLIENT:[ " + sClient_IP + " ][ " + dp.getData()[1] + " ]");
        _notifyUI("[[UDP]] FIND CLIENT:[ " + sClient_IP + " ][ " + dp.getData()[1] + " ]");
//        byte[] test2 ={0x06,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C};
//        Send(test2);

        return;
    }

    protected void finalize() throws java.lang.Throwable {
        super.finalize();
    }
}
