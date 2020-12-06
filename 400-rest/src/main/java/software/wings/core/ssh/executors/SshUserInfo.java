package software.wings.core.ssh.executors;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Created by anubhaw on 6/21/16.
 */
public class SshUserInfo implements UserInfo, UIKeyboardInteractive {
  private String password;
  private String passphrase;

  /**
   * Instantiates a new Ssh user info.
   *
   * @param password the password
   */
  public SshUserInfo(String password) {
    this.password = password;
  }

  @Override
  public String getPassphrase() {
    return passphrase;
  }

  /**
   * Sets passphrase.
   *
   * @param passphrase the passphrase
   */
  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  @Override
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean promptPassword(String message) {
    return true;
  }

  @Override
  public boolean promptPassphrase(String message) {
    return true;
  }

  @Override
  public boolean promptYesNo(String message) {
    return true;
  }

  @Override
  public void showMessage(String message) {
    // do nothing
  }

  @Override
  public String[] promptKeyboardInteractive(
      String destination, String name, String instruction, String[] prompt, boolean[] echo) {
    if (prompt.length != 1 || echo[0] || this.password == null) {
      return null;
    }
    String[] response = new String[1];
    response[0] = this.password;
    return response;
  }
}
