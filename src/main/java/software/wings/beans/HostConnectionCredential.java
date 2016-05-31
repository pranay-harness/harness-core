package software.wings.beans;

/**
 * Created by anubhaw on 5/31/16.
 */
public class HostConnectionCredential {
  private String sshUser;
  private String sshPassword;
  private String appAccount;
  private String appAccountPassword;

  public String getSshUser() {
    return sshUser;
  }

  public void setSshUser(String sshUser) {
    this.sshUser = sshUser;
  }

  public String getSshPassword() {
    return sshPassword;
  }

  public void setSshPassword(String sshPassword) {
    this.sshPassword = sshPassword;
  }

  public String getAppAccount() {
    return appAccount;
  }

  public void setAppAccount(String appAccount) {
    this.appAccount = appAccount;
  }

  public String getAppAccountPassword() {
    return appAccountPassword;
  }

  public void setAppAccountPassword(String appAccountPassword) {
    this.appAccountPassword = appAccountPassword;
  }
}
