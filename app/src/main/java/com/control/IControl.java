package com.control;

public interface IControl {
    String sAddress = "/dev/ttyS0";   //Serial Port
    int iCMDDelay = 4;                //Command delay time MS
    int iBaudRate = 115200;           //Baud-rate
    boolean bUseMonitor = true;      //Used internal monitor for test

    int GRADE_TYPE_0 = 0; //Rise and fall
    int GRADE_TYPE_1 = 1; //Firefighting
    int GRADE_TYPE_MIN = GRADE_TYPE_0;
    int GRADE_TYPE_MAX = GRADE_TYPE_1;
    int GRADE_0_MAX_LEN = 13;  //grade size + 1
    int GRADE_1_MAX_LEN = 4;   //grade size + 1
    String CFG_ITEM_HOSTID = "HostID";
    String CFG_FILE_NAME = "Setting";

    String getGrade(int type);
    void testForUDPCmd(int iCMD);
}
