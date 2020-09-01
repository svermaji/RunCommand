package com.sv.runcmd;

import javax.swing.*;

public class AppButton extends JButton {

    AppButton(String text, char mnemonic) {
        setText(text);
        setMnemonic(mnemonic);
    }
}
