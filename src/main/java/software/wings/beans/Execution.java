package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.CommandUnit.ExecutionResult;

import java.util.List;

/**
 * The Class Execution.
 */
@Entity(value = "executions")
public abstract class Execution extends Base {
  @Reference(idOnly = true, ignoreMissing = true) private Host host;
  private String sshUser;
  private String sshPassword;
  private String appAccount;
  private String appAccountPassword;
  private ExecutionResult executionResult;

  /**
   * Gets host.
   *
   * @return the host
   */
  public Host getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(Host host) {
    this.host = host;
  }

  /**
   * Gets ssh user.
   *
   * @return the ssh user
   */
  public String getSshUser() {
    return sshUser;
  }

  /**
   * Sets ssh user.
   *
   * @param sshUser the ssh user
   */
  public void setSshUser(String sshUser) {
    this.sshUser = sshUser;
  }

  /**
   * Gets ssh password.
   *
   * @return the ssh password
   */
  public String getSshPassword() {
    return sshPassword;
  }

  /**
   * Sets ssh password.
   *
   * @param sshPassword the ssh password
   */
  public void setSshPassword(String sshPassword) {
    this.sshPassword = sshPassword;
  }

  /**
   * Gets app account.
   *
   * @return the app account
   */
  public String getAppAccount() {
    return appAccount;
  }

  /**
   * Sets app account.
   *
   * @param appAccount the app account
   */
  public void setAppAccount(String appAccount) {
    this.appAccount = appAccount;
  }

  /**
   * Gets app account password.
   *
   * @return the app account password
   */
  public String getAppAccountPassword() {
    return appAccountPassword;
  }

  /**
   * Sets app account password.
   *
   * @param appAccountPassword the app account password
   */
  public void setAppAccountPassword(String appAccountPassword) {
    this.appAccountPassword = appAccountPassword;
  }

  /**
   * Gets command units.
   *
   * @return the command units
   */
  public abstract List<CommandUnit> getCommandUnits();

  /**
   * Gets execution result.
   *
   * @return the execution result
   */
  public ExecutionResult getExecutionResult() {
    return executionResult;
  }

  /**
   * Sets execution result.
   *
   * @param executionResult the execution result
   */
  public void setExecutionResult(ExecutionResult executionResult) {
    this.executionResult = executionResult;
  }
}
