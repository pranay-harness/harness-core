package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;
@Entity(value = "executions", noClassnameStored = true)
public abstract class Execution extends Base {
  @Reference(idOnly = true, ignoreMissing = true) private Host host;
  private CommandType commandType;
  private Strategy strategy = Strategy.SERIAL;
  private String sshUser;
  private String sshPassword;
  private String appAccount;
  private String appAccountPassword;

  public CommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(CommandType commandType) {
    this.commandType = commandType;
  }

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

  public Strategy getStrategy() {
    return strategy;
  }

  public void setStrategy(Strategy strategy) {
    this.strategy = strategy;
  }

  public abstract String getCommand();

  public abstract List<CommandUnit> getCommandUnits();

  public abstract String getSetupCommand();

  public abstract String getDeployCommand();

  public enum CommandType { DEPLOY, START, STOP, RESTART, ENABLE, DISABLE, ROLLBACK, CUSTOM }

  public enum Strategy { SERIAL, PARALLEL }
}
