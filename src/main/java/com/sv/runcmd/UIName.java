package com.sv.runcmd;

public enum UIName {
    LBL_FILTER("Filter", 'F'),
    LBL_R_FILTERS("Recent", 'R', "Recent used filters."),
    MNU_COPY("Copy Command", 'C'),
    MNU_CFG("Open config location", 'G'),
    MNU_CMD("Open command", 'D'),
    MNU_TIMER_CANCEL("Cancel Timer", 'r', "Cancel timer"),
    MNU_CLOSE_15("Close in 15 min", '5', "Run 'close' command after 15 min"),
    MNU_CLOSE_30("Close in 30 min", '0', "Run 'close' command after 30 min");

    String name, tip;
    char mnemonic;

    UIName(String name, char mnemonic) {
        this(name, mnemonic, null);
    }

    UIName(String name, char mnemonic, String tip) {
        this.name = name;
        this.tip = tip;
        this.mnemonic = mnemonic;
    }


}
