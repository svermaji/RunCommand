package com.sv.runcmd.helpers;

import javax.swing.*;

public class KeyActionDetails {

    private final int keyEvent, inputEvent;
    private final Action action;

    public KeyActionDetails(int keyEvent, int inputEvent, Action action) {
        this.keyEvent = keyEvent;
        this.inputEvent = inputEvent;
        this.action = action;
    }

    public int getKeyEvent() {
        return keyEvent;
    }

    public int getInputEvent() {
        return inputEvent;
    }

    public Action getAction() {
        return action;
    }
}
