package com.control;

import static com.control.IControl.GRADE_0_MAX_LEN;
import static com.control.IControl.GRADE_1_MAX_LEN;
import static com.control.IControl.GRADE_TYPE_0;
import static com.control.IControl.GRADE_TYPE_1;

import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class DataBase {
    public static final int REFRESHED = 1;
    public static final int NON_REFRESHED = 0;

    private final String TAG = "[FJY DB]";
    private ArrayList<DataItem> DataList = new ArrayList<>();
    private static final DataBase instance = new DataBase();

    public static DataBase getInstance(){
        return instance;
    }
    private DataBase() {
    }

    public int AddNewDataItem(int size){
        DataList.add(new DataItem(size));
        return DataList.size() - 1;
    }

    public boolean SetData(int id, byte[] data, int offset){
        DataItem DI = _getDataItem(id);
        if(null == DI || offset < 0 || data.length <= offset)
        {
            Log.e(TAG, "SetData ERROR !!! [" + id + "]");
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
        DataItem di = _getDataItem(type);
        byte[] data = null;
        if(null == di)
        {
            return null;
        }

        int iDataSize = _getDataSize(type);
        if(0 == iDataSize){
            return null;
        }

        data = new byte[iDataSize];

        di.lock.lock();
        System.arraycopy(di.data,0, data,0,iDataSize);
        Log.v(TAG, "GetData:[ " + data.length + " ][ " + HexConverUtils.bytesToHex(data) + " ]");
        di.data[0] = NON_REFRESHED;
        Log.v(TAG, "DBItem change to non-refreshed:[ " + di.data.length + " ][ " + HexConverUtils.bytesToHex(di.data) + " ]");
        di.lock.unlock();

        return data;
    }

    private DataItem _getDataItem(int id){
        if(0 > id || DataList.size() <= id){
            Log.e(TAG, "Data Type ERROR !!! [ " + id + " ]");
            return null;
        }

        return DataList.get(id);
    }

    private int _getDataSize(int id){
        if(0 > id || DataList.size() <= id){
            Log.e(TAG, "Data Size Type ERROR !!! [ " + id + " ]");
            return 0;
        }

        return DataList.get(id).data.length;
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

