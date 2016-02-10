package software.wings.core.executors;

import com.jcraft.jsch.UserInfo;

/**
 * Created by anubhaw on 2/4/16.
 */

public class MyUserInfo implements UserInfo {
  private String sshPassword;
  public MyUserInfo(String sshPassword) {
    this.sshPassword = sshPassword;
  }
  public String getPassword() {
    return sshPassword;
  }
  public boolean promptYesNo(String str) {
    return true;
  }
  public String getPassphrase() {
    return null;
  }
  public boolean promptPassphrase(String message) {
    return true;
  }
  public boolean promptPassword(String message) {
    return true;
  }
  public void showMessage(String message) {
    System.out.println(message);
  }
}
