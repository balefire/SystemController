package com.control;

import static com.control.IControl.GRADE_0_MAX_LEN;
import static com.control.IControl.GRADE_1_MAX_LEN;
import static com.control.IControl.GRADE_TYPE_0;
import static com.control.IControl.GRADE_TYPE_1;
import android.util.Log;
import java.util.concurrent.locks.ReentrantLock;

public class GradeData {
    public static final int REFRESHED = 1;
    public static final int NON_REFRESHED = 0;

    private final String TAG = "[FJY Grade]";
    private GradeArray ga1 = new GradeArray(GRADE_0_MAX_LEN);
    private GradeArray ga2 = new GradeArray(GRADE_1_MAX_LEN);
    private static final GradeData instance = new GradeData();

    public static GradeData getInstance(){
        return instance;
    }
    private GradeData() {
    }

    public boolean SetGrade(int type, byte[] data, int offset){
        GradeArray ga = getGA(type);
        if(null == ga || offset < 0 || data.length <= offset)
        {
            Log.e(TAG, "SetGrade ERROR !!! [" + type + "]");
            return false;
        }

        ga.lock.lock();
        System.arraycopy(data,offset,ga.data,1,ga.data.length - 1);
        ga.data[0] = REFRESHED;
        Log.v(TAG, "GA Refreshed:[ " + ga.data.length + " ][ " + HexConverUtils.bytesToHex(ga.data) + " ]");
        ga.lock.unlock();
        return true;
    }

    public byte[] GetGrade(int type){
        GradeArray ga = getGA(type);
        byte[] data = null;
        if(null == ga)
        {
            return null;
        }

        int iDataSize = getDataSize(type);
        if(0 == iDataSize){
            return null;
        }

        data = new byte[iDataSize];

        ga.lock.lock();
        System.arraycopy(ga.data,0, data,0,iDataSize);
        Log.v(TAG, "GetGrade:[ " + data.length + " ][ " + HexConverUtils.bytesToHex(data) + " ]");
        ga.data[0] = NON_REFRESHED;
        Log.v(TAG, "GA change to non-refreshed:[ " + ga.data.length + " ][ " + HexConverUtils.bytesToHex(ga.data) + " ]");
        ga.lock.unlock();

        return data;
    }

    private GradeArray getGA(int type){
        if(GRADE_TYPE_0 == type){
            return ga1;
        }
        else if(GRADE_TYPE_1 == type){
            return ga2;
        }
        else{
            Log.e(TAG, "Data Type ERROR !!! [ " + type + " ]");
            return null;
        }
    }

    private int getDataSize(int type){
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

class GradeArray {
    public byte[] data = null;
    public ReentrantLock lock = new ReentrantLock();

    public GradeArray(int DataSize) {
        data = new byte[DataSize];
    }

    private GradeArray() {
    }
}

