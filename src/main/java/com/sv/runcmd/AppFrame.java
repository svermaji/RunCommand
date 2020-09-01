package com.sv.runcmd;

import javax.swing.*;
import java.awt.*;

public class AppFrame extends JFrame {

    AppFrame() {
        Font baseFont = new Font("Dialog", Font.PLAIN, 12);
        setFont(baseFont);
        setLocationRelativeTo(null);
        setBackground(Color.WHITE);
        setForeground(Color.black);
        setLayout(new FlowLayout());
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
