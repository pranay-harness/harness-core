package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;
@Entity(value = "executions")
public abstract class Execution extends Base {
  @Reference(idOnly = true, ignoreMissing = true) private Host host;
  private String sshUser;
  private String sshPassword;
  private String appAccount;
  private String appAccountPassword;

  public Host getHost() {
    return host;
  }

  public void setHost(Host host) {
    this.host = host;
  }

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
  public abstract List<CommandUnit> getCommandUnits();
}
