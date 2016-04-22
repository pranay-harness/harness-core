/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate remote exec. $ CLASSPATH=.:../build javac Exec.java $
 * CLASSPATH=.:../build java Exec You will be asked username, hostname, displayname, passwd and
 * command. If everything works fine, given command will be invoked on the remote side and outputs
 * will be printed out.
 */
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.InputStream;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class Exec {
  public static void main(String[] arg) {
    try {
      JSch jsch = new JSch();

      Session session = jsch.getSession("vagrant", "localhost", 2222);

      /*
      String xhost="127.0.0.1";
      int xport=0;
      String display=JOptionPane.showInputDialog("Enter display name",
                                                 xhost+":"+xport);
      xhost=display.substring(0, display.indexOf(':'));
      xport=Integer.parseInt(display.substring(display.indexOf(':')+1));
      session.setX11Host(xhost);
      session.setX11Port(xport+6000);
      */

      // username and password will be given via UserInfo interface.
      UserInfo ui = new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      String command = JOptionPane.showInputDialog("Enter command", "set|grep SSH");

      Channel channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // X Forwarding
      // channel.setXForwarding(true);

      channel.setInputStream(System.in);
      // channel.setInputStream(null);

      channel.setOutputStream(System.out);

      // FileOutputStream fos=new FileOutputStream("/tmp/stderr");
      //((ChannelExec)channel).setErrStream(fos);
      ((ChannelExec) channel).setErrStream(System.err);

      InputStream in = channel.getInputStream();

      channel.connect();

      byte[] tmp = new byte[1024];
      while (true) {
        while (in.available() > 0) {
          int i = in.read(tmp, 0, 1024);
          if (i < 0)
            break;
          System.out.print(new String(tmp, 0, i));
        }
        if (channel.isClosed()) {
          if (in.available() > 0)
            continue;
          System.out.println("exit-status: " + channel.getExitStatus());
          break;
        }
        try {
          Thread.sleep(1000);
        } catch (Exception ee) {
        }
      }
      channel.disconnect();
      session.disconnect();
    } catch (Exception ex) {
      System.out.println(ex);
    }
  }

  public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
    final GridBagConstraints gbc = new GridBagConstraints(
        0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    String passwd;
    JTextField passwordField = new JPasswordField(20);
    private Container panel;

    @Override
    public String getPassphrase() {
      return null;
    }

    @Override
    public String getPassword() {
      return passwd;
    }

    @Override
    public boolean promptPassword(String message) {
      Object[] ob = {passwordField};
      int result = JOptionPane.showConfirmDialog(null, ob, message, JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        passwd = passwordField.getText();
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean promptPassphrase(String message) {
      return true;
    }

    @Override
    public boolean promptYesNo(String str) {
      Object[] options = {"yes", "no"};
      int foo = JOptionPane.showOptionDialog(
          null, str, "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      return foo == 0;
    }

    @Override
    public void showMessage(String message) {
      JOptionPane.showMessageDialog(null, message);
    }

    @Override
    public String[] promptKeyboardInteractive(
        String destination, String name, String instruction, String[] prompt, boolean[] echo) {
      panel = new JPanel();
      panel.setLayout(new GridBagLayout());

      gbc.weightx = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx = 0;
      panel.add(new JLabel(instruction), gbc);
      gbc.gridy++;

      gbc.gridwidth = GridBagConstraints.RELATIVE;

      JTextField[] texts = new JTextField[prompt.length];
      for (int i = 0; i < prompt.length; i++) {
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 1;
        panel.add(new JLabel(prompt[i]), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        if (echo[i]) {
          texts[i] = new JTextField(20);
        } else {
          texts[i] = new JPasswordField(20);
        }
        panel.add(texts[i], gbc);
        gbc.gridy++;
      }

      if (JOptionPane.showConfirmDialog(
              null, panel, destination + ": " + name, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
          == JOptionPane.OK_OPTION) {
        String[] response = new String[prompt.length];
        for (int i = 0; i < prompt.length; i++) {
          response[i] = texts[i].getText();
        }
        return response;
      } else {
        return null; // cancel
      }
    }
  }
}
