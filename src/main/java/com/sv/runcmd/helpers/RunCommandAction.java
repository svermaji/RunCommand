package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RunCommandAction extends AbstractAction {

    private final RunCommandUI rc;
    private final JTable table;

    public RunCommandAction(RunCommandUI rc, JTable table) {
        this.rc = rc;
        this.table = table;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        rc.execCommand(table.getValueAt(table.getSelectedRow(), 0).toString());
    }
}
