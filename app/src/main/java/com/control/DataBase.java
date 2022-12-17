package com.control;

import static com.control.IControl.GRADE_0_MAX_LEN;
import static com.control.IControl.GRADE_1_MAX_LEN;
import static com.control.IControl.GRADE_TYPE_0;
import static com.control.IControl.GRADE_TYPE_1;
import android.util.Log;
import java.util.concurrent.locks.ReentrantLock;

public class DataBase {
    public static final int REFRESHED = 1;
    public static final int NON_REFRESHED = 0;

    private final String TAG = "[FJY DB]";
    private DataItem DI1 = new DataItem(GRADE_0_MAX_LEN);
    private DataItem DI2 = new DataItem(GRADE_1_MAX_LEN);
    private static final DataBase instance = new DataBase();

    public static DataBase getInstance(){
        return instance;
    }
    private DataBase() {
    }

    public boolean SetData(int type, byte[] data, int offset){
        DataItem DI = _getDataItem(type);
        if(null == DI || offset < 0 || data.length <= offset)
        {
            Log.e(TAG, "SetData ERROR !!! [" + type + "]");
            return false;
        }

        DI.lock.lock();
        System.arraycopy(data,offset,DI.data,1,DI.data.length - 1);
        DI.data[0] = REFRESHED;
        Log.v(TAG, "DBItem Refreshed:[ " + DI.data.length + " ][ " + HexConverUtils.bytesToHex(DI.data) + " ]");
        DI.lock.unlock();
        return true;
    }

    public byte[] GetData(int type){
        DataItem DI = _getDataItem(type);
        byte[] data = null;
        if(null == DI)
        {
            return null;
        }

        int iDataSize = _getDataSize(type);
        if(0 == iDataSize){
            return null;
        }

        data = new byte[iDataSize];

        DI.lock.lock();
        System.arraycopy(DI.data,0, data,0,iDataSize);
        Log.v(TAG, "GetData:[ " + data.length + " ][ " + HexConverUtils.bytesToHex(data) + " ]");
        DI.data[0] = NON_REFRESHED;
        Log.v(TAG, "DBItem change to non-refreshed:[ " + DI.data.length + " ][ " + HexConverUtils.bytesToHex(DI.data) + " ]");
        DI.lock.unlock();

        return data;
    }

    private DataItem _getDataItem(int type){
        if(GRADE_TYPE_0 == type){
            return DI1;
        }
        else if(GRADE_TYPE_1 == type){
            return DI2;
        }
        else{
            Log.e(TAG, "Data Type ERROR !!! [ " + type + " ]");
            return null;
        }
    }

    private int _getDataSize(int type){
        if(GRADE_TYPE_0 == type){
            return GRADE_0_MAX_LEN;
        }
        else if(GRADE_TYPE_1 == type){
            return GRADE_1_MAX_LEN;
        }
        else{
            Log.e(TAG, "Data Size Type ERROR !!! [ " + type + " ]");
            return 0;
        }
    }

}

class DataItem {
    public byte[] data = null;
    public ReentrantLock lock = new ReentrantLock();

    public DataItem(int DataSize) {
        data = new byte[DataSize];
    }

    private DataItem() {
    }
}

