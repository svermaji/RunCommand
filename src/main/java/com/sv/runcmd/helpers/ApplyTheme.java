package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import javax.swing.*;

public class ApplyTheme extends SwingWorker<Integer, String> {

    private RunCommandUI obj;

    public ApplyTheme(RunCommandUI obj) {
        this.obj = obj;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        obj.applyLookAndFeel();
        return 1;
    }
}
