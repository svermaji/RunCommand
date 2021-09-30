package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RecentMenuAction extends AbstractAction {

    private final JTextField dest;
    private final String val;

    public RecentMenuAction(JTextField dest, String val) {
        this.dest = dest;
        this.val = val;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dest.setText(val);
        dest.transferFocus();
    }
}
