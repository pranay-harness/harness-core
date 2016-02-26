package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

public abstract class Execution extends Base {
  private List<String> hostInstanceMappings = new ArrayList<>();

  public enum OperationType { DEPLOY, START, STOP, RESTART, ENABLE, DISABLE, ROLLBACK, CUSTOM }

  private OperationType operationType;

  public enum Strategy { SERIAL, PARALLEL }

  private Strategy strategy = Strategy.SERIAL;

  private String sshUser;
  private String sshPassword;
  private String appAccount;
  private String appAccountPassword;

  public OperationType getOperationType() {
    return operationType;
  }
  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }
  public List<String> getHostInstanceMappings() {
    return hostInstanceMappings;
  }
  public void setHostInstanceMappings(List<String> hostInstanceMappings) {
    this.hostInstanceMappings = hostInstanceMappings;
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
  public Strategy getStrategy() {
    return strategy;
  }
  public void setStrategy(Strategy strategy) {
    this.strategy = strategy;
  }

  public abstract String getCommand();
  public abstract String getSetupCommand();
  public abstract String getDeployCommand();
}
