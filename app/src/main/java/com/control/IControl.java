package com.control;

public interface IControl {

    //Serial port configuration.
    String sAddress = "/dev/ttyS0";         //Serial Port
    int iCMDDelay = 4;                      //Command delay time MS
    int iBaudRate = 115200;                 //Baud-rate

    //Test configuration.
    boolean bUseMonitorApp = true;          //Used internal monitor for test
    boolean bTestNewGetIpMethod = true;     //Test new function for get local ip.

    //Config file configuration.
    String CFG_ITEM_HOSTID = "HostID";      //Key name for host id in config file.
    String CFG_FILE_NAME = "Setting";       //File name for config file.

    //Component control
    boolean bModuleEnabled_Grade = true;     //Grade system enabled/disabled.
    boolean bModuleEnable_HighVol = false;   //High Voltage system enabled/disabled.

    //Grade configuration.
    int GRADE_TYPE_0 = 0;                   //Get grade for [Rise and fall]
    int GRADE_TYPE_1 = 1;                   //Get grade for [Firefighting]
    int GRADE_ITEM_MIN = GRADE_TYPE_0;
    int GRADE_ITEM_MAX = GRADE_TYPE_1;
    int GRADE_0_MAX_LEN = 13;               //Grade size + 1 (First byte is Effective-Set)
    int GRADE_1_MAX_LEN = 4;                //Grade size + 1 (First byte is Effective-Set)

    String getGrade(int iGradeType);        //Get examination grade information.

    //Just for test UDP client commands, DON'T used.
    void testForUDPClient(int iCMD);        //Test function for send command to UDP client directly.

}
