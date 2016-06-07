/**
 *
 */

package software.wings.beans;

/**
 * The type Ssh execution credential.
 *
 * @author Rishi
 */
public class SSHExecutionCredential extends ExecutionCredential {
  private String sshUser;
  private String sshPassword;
  private String appAccount;
  private String appAccountPassword;

  /**
   * Instantiates a new Ssh execution credential.
   */
  public SSHExecutionCredential() {
    super(ExecutionType.SSH);
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
}
