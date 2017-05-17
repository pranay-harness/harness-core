/**
 *
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * The type Ssh execution credential.
 *
 * @author Rishi
 */
@JsonTypeName("SSH")
public class SSHExecutionCredential extends ExecutionCredential {
  private String sshUser;
  private char[] sshPassword;
  private String appAccount;
  private char[] appAccountPassword;
  private char[] keyPassphrase;

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
  public char[] getSshPassword() {
    return sshPassword;
  }

  /**
   * Sets ssh password.
   *
   * @param sshPassword the ssh password
   */
  public void setSshPassword(char[] sshPassword) {
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
  public char[] getAppAccountPassword() {
    return appAccountPassword;
  }

  /**
   * Sets app account password.
   *
   * @param appAccountPassword the app account password
   */
  public void setAppAccountPassword(char[] appAccountPassword) {
    this.appAccountPassword = appAccountPassword;
  }

  /**
   * Gets key passphrase.
   *
   * @return the key passphrase
   */
  public char[] getKeyPassphrase() {
    return keyPassphrase;
  }

  /**
   * Sets key passphrase.
   *
   * @param keyPassphrase the key passphrase
   */
  public void setKeyPassphrase(char[] keyPassphrase) {
    this.keyPassphrase = keyPassphrase;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String sshUser;
    private char[] sshPassword;
    private String appAccount;
    private char[] appAccountPassword;
    private char[] keyPassphrase;
    private ExecutionType executionType;

    private Builder() {}

    /**
     * A ssh execution credential builder.
     *
     * @return the builder
     */
    public static Builder aSSHExecutionCredential() {
      return new Builder();
    }

    /**
     * With ssh user builder.
     *
     * @param sshUser the ssh user
     * @return the builder
     */
    public Builder withSshUser(String sshUser) {
      this.sshUser = sshUser;
      return this;
    }

    /**
     * With ssh password builder.
     *
     * @param sshPassword the ssh password
     * @return the builder
     */
    public Builder withSshPassword(char[] sshPassword) {
      this.sshPassword = sshPassword;
      return this;
    }

    /**
     * With app account builder.
     *
     * @param appAccount the app account
     * @return the builder
     */
    public Builder withAppAccount(String appAccount) {
      this.appAccount = appAccount;
      return this;
    }

    /**
     * With app account password builder.
     *
     * @param appAccountPassword the app account password
     * @return the builder
     */
    public Builder withAppAccountPassword(char[] appAccountPassword) {
      this.appAccountPassword = appAccountPassword;
      return this;
    }

    /**
     * With key passphrase builder.
     *
     * @param keyPassphrase the key passphrase
     * @return the builder
     */
    public Builder withKeyPassphrase(char[] keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    /**
     * With execution type builder.
     *
     * @param executionType the execution type
     * @return the builder
     */
    public Builder withExecutionType(ExecutionType executionType) {
      this.executionType = executionType;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSSHExecutionCredential()
          .withSshUser(sshUser)
          .withSshPassword(sshPassword)
          .withAppAccount(appAccount)
          .withAppAccountPassword(appAccountPassword)
          .withKeyPassphrase(keyPassphrase)
          .withExecutionType(executionType);
    }

    /**
     * Build ssh execution credential.
     *
     * @return the ssh execution credential
     */
    public SSHExecutionCredential build() {
      SSHExecutionCredential sSHExecutionCredential = new SSHExecutionCredential();
      sSHExecutionCredential.setSshUser(sshUser);
      sSHExecutionCredential.setSshPassword(sshPassword);
      sSHExecutionCredential.setAppAccount(appAccount);
      sSHExecutionCredential.setAppAccountPassword(appAccountPassword);
      sSHExecutionCredential.setKeyPassphrase(keyPassphrase);
      sSHExecutionCredential.setExecutionType(executionType);
      return sSHExecutionCredential;
    }
  }
}
