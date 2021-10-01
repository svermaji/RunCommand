package com.sv.runcmd;

import java.util.Arrays;

public enum UIName {
    LBL_FILTER("Filter", 'F'),
    LBL_R_FILTERS("*", 'R', "Recent used filters."),
    BTN_RELOAD("Reload", 'O'),
    BTN_CLEAR("Clear", 'E'),
    BTN_LOCK("<html>&#x1F512;</html>", 'K', "Lock the screen"),
    MNU_COPY("Copy Command", 'C'),
    MNU_CFG("Open config location", 'G'),
    MNU_CMD("Open command", 'D'),
    MNU_TIMER_CANCEL("Cancel Timer", 'c', "Cancel timer"),
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
