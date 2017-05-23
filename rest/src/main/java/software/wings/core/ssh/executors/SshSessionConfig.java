package software.wings.core.ssh.executors;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshSessionConfig implements Encryptable {
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotEmpty private ExecutorType executorType;
  @NotEmpty private String executionId;
  @NotEmpty private String commandUnitName;
  private Integer socketConnectTimeout = (int) TimeUnit.SECONDS.toMillis(30);
  private Integer sshConnectionTimeout = (int) TimeUnit.MINUTES.toMillis(2);
  private Integer sshSessionTimeout = (int) TimeUnit.MINUTES.toMillis(10);
  private Integer retryInterval;
  @NotEmpty private String host;
  private Integer port = 22;
  private String userName;
  @Encrypted private char[] password;
  private String keyName;
  @Encrypted private char[] key;
  @Encrypted private char[] keyPassphrase;
  private String sudoAppName;
  @Encrypted private char[] sudoAppPassword;
  private SshSessionConfig bastionHostConfig;

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets executor type.
   *
   * @return the executor type
   */
  public ExecutorType getExecutorType() {
    return executorType;
  }

  /**
   * Sets executor type.
   *
   * @param executorType the executor type
   */
  public void setExecutorType(ExecutorType executorType) {
    this.executorType = executorType;
  }

  /**
   * Gets execution id.
   *
   * @return the execution id
   */
  public String getExecutionId() {
    return executionId;
  }

  /**
   * Sets execution id.
   *
   * @param executionId the execution id
   */
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  /**
   * Gets ssh connection timeout.
   *
   * @return the ssh connection timeout
   */
  public Integer getSshConnectionTimeout() {
    return sshConnectionTimeout;
  }

  /**
   * Sets ssh connection timeout.
   *
   * @param sshConnectionTimeout the ssh connection timeout
   */
  public void setSshConnectionTimeout(Integer sshConnectionTimeout) {
    this.sshConnectionTimeout = sshConnectionTimeout;
  }

  /**
   * Gets ssh session timeout.
   *
   * @return the ssh session timeout
   */
  public Integer getSshSessionTimeout() {
    return sshSessionTimeout;
  }

  /**
   * Sets ssh session timeout.
   *
   * @param sshSessionTimeout the ssh session timeout
   */
  public void setSshSessionTimeout(Integer sshSessionTimeout) {
    this.sshSessionTimeout = sshSessionTimeout;
  }

  /**
   * Gets retry interval.
   *
   * @return the retry interval
   */
  public Integer getRetryInterval() {
    return retryInterval;
  }

  /**
   * Sets retry interval.
   *
   * @param retryInterval the retry interval
   */
  public void setRetryInterval(Integer retryInterval) {
    this.retryInterval = retryInterval;
  }

  /**
   * Gets host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets port.
   *
   * @return the port
   */
  public Integer getPort() {
    return port;
  }

  /**
   * Sets port.
   *
   * @param port the port
   */
  public void setPort(Integer port) {
    this.port = port;
  }

  /**
   * Gets user name.
   *
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets user name.
   *
   * @param userName the user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Gets password.
   *
   * @return the password
   */
  public char[] getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  public void setPassword(char[] password) {
    this.password = password;
  }

  /**
   * Gets key.
   *
   * @return the key
   */
  public char[] getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(char[] key) {
    this.key = key;
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
   * Gets sudo app name.
   *
   * @return the sudo app name
   */
  public String getSudoAppName() {
    return sudoAppName;
  }

  /**
   * Sets sudo app name.
   *
   * @param sudoAppName the sudo app name
   */
  public void setSudoAppName(String sudoAppName) {
    this.sudoAppName = sudoAppName;
  }

  /**
   * Gets sudo app password.
   *
   * @return the sudo app password
   */
  public char[] getSudoAppPassword() {
    return sudoAppPassword;
  }

  /**
   * Sets sudo app password.
   *
   * @param sudoAppPassword the sudo app password
   */
  public void setSudoAppPassword(char[] sudoAppPassword) {
    this.sudoAppPassword = sudoAppPassword;
  }

  /**
   * Gets bastion host config.
   *
   * @return the bastion host config
   */
  public SshSessionConfig getBastionHostConfig() {
    return bastionHostConfig;
  }

  /**
   * Sets bastion host config.
   *
   * @param bastionHostConfig the bastion host config
   */
  public void setBastionHostConfig(SshSessionConfig bastionHostConfig) {
    this.bastionHostConfig = bastionHostConfig;
  }

  /**
   * Gets command unit name.
   *
   * @return the command unit name
   */
  public String getCommandUnitName() {
    return commandUnitName;
  }

  /**
   * Sets command unit name.
   *
   * @param commandUnitName the command unit name
   */
  public void setCommandUnitName(String commandUnitName) {
    this.commandUnitName = commandUnitName;
  }

  /**
   * Gets socket connect timeout.
   *
   * @return the socket connect timeout
   */
  public Integer getSocketConnectTimeout() {
    return socketConnectTimeout;
  }

  /**
   * Sets socket connect timeout.
   *
   * @param socketConnectTimeout the socket connect timeout
   */
  public void setSocketConnectTimeout(Integer socketConnectTimeout) {
    this.socketConnectTimeout = socketConnectTimeout;
  }

  /**
   * Gets key name.
   *
   * @return the key name
   */
  public String getKeyName() {
    return keyName;
  }

  /**
   * Sets key name.
   *
   * @param keyName the key name
   */
  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  @Override
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, appId, executorType, executionId, commandUnitName, socketConnectTimeout,
        sshConnectionTimeout, sshSessionTimeout, retryInterval, host, port, userName, password, keyName, key,
        keyPassphrase, sudoAppName, sudoAppPassword, bastionHostConfig);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SshSessionConfig other = (SshSessionConfig) obj;
    return Objects.equals(this.accountId, other.accountId) && Objects.equals(this.appId, other.appId)
        && Objects.equals(this.executorType, other.executorType) && Objects.equals(this.executionId, other.executionId)
        && Objects.equals(this.commandUnitName, other.commandUnitName)
        && Objects.equals(this.socketConnectTimeout, other.socketConnectTimeout)
        && Objects.equals(this.sshConnectionTimeout, other.sshConnectionTimeout)
        && Objects.equals(this.sshSessionTimeout, other.sshSessionTimeout)
        && Objects.equals(this.retryInterval, other.retryInterval) && Objects.equals(this.host, other.host)
        && Objects.equals(this.port, other.port) && Objects.equals(this.userName, other.userName)
        && Arrays.equals(this.password, other.password) && Objects.equals(this.keyName, other.keyName)
        && Arrays.equals(this.key, other.key) && Arrays.equals(this.keyPassphrase, other.keyPassphrase)
        && Objects.equals(this.sudoAppName, other.sudoAppName)
        && Arrays.equals(this.sudoAppPassword, other.sudoAppPassword)
        && Objects.equals(this.bastionHostConfig, other.bastionHostConfig);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("appId", appId)
        .add("executorType", executorType)
        .add("executionId", executionId)
        .add("commandUnitName", commandUnitName)
        .add("socketConnectTimeout", socketConnectTimeout)
        .add("sshConnectionTimeout", sshConnectionTimeout)
        .add("sshSessionTimeout", sshSessionTimeout)
        .add("retryInterval", retryInterval)
        .add("host", host)
        .add("port", port)
        .add("userName", userName)
        .add("keyName", keyName)
        .add("sudoAppName", sudoAppName)
        .add("bastionHostConfig", bastionHostConfig)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accountId;
    private String appId;
    private ExecutorType executorType;
    private String executionId;
    private String commandUnitName;
    private Integer socketConnectTimeout = (int) TimeUnit.SECONDS.toMillis(30);
    private Integer sshConnectionTimeout = (int) TimeUnit.MINUTES.toMillis(2);
    private Integer sshSessionTimeout = (int) TimeUnit.MINUTES.toMillis(10);
    private Integer retryInterval;
    private String host;
    private Integer port = 22;
    private String userName;
    private char[] password;
    private String keyName;
    private char[] key;
    private char[] keyPassphrase;
    private String sudoAppName;
    private char[] sudoAppPassword;
    private SshSessionConfig bastionHostConfig;

    private Builder() {}

    /**
     * A ssh session config builder.
     *
     * @return the builder
     */
    public static Builder aSshSessionConfig() {
      return new Builder();
    }

    /**
     * With account id builder.
     *
     * @param accountId the account id
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With executor type builder.
     *
     * @param executorType the executor type
     * @return the builder
     */
    public Builder withExecutorType(ExecutorType executorType) {
      this.executorType = executorType;
      return this;
    }

    /**
     * With execution id builder.
     *
     * @param executionId the execution id
     * @return the builder
     */
    public Builder withExecutionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    /**
     * With command unit name builder.
     *
     * @param commandUnitName the command unit name
     * @return the builder
     */
    public Builder withCommandUnitName(String commandUnitName) {
      this.commandUnitName = commandUnitName;
      return this;
    }

    /**
     * With socket connect timeout builder.
     *
     * @param socketConnectTimeout the socket connect timeout
     * @return the builder
     */
    public Builder withSocketConnectTimeout(Integer socketConnectTimeout) {
      this.socketConnectTimeout = socketConnectTimeout;
      return this;
    }

    /**
     * With ssh connection timeout builder.
     *
     * @param sshConnectionTimeout the ssh connection timeout
     * @return the builder
     */
    public Builder withSshConnectionTimeout(Integer sshConnectionTimeout) {
      this.sshConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    /**
     * With ssh session timeout builder.
     *
     * @param sshSessionTimeout the ssh session timeout
     * @return the builder
     */
    public Builder withSshSessionTimeout(Integer sshSessionTimeout) {
      this.sshSessionTimeout = sshSessionTimeout;
      return this;
    }

    /**
     * With retry interval builder.
     *
     * @param retryInterval the retry interval
     * @return the builder
     */
    public Builder withRetryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    /**
     * With host builder.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(String host) {
      this.host = host;
      return this;
    }

    /**
     * With port builder.
     *
     * @param port the port
     * @return the builder
     */
    public Builder withPort(Integer port) {
      this.port = port;
      return this;
    }

    /**
     * With user name builder.
     *
     * @param userName the user name
     * @return the builder
     */
    public Builder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    /**
     * With password builder.
     *
     * @param password the password
     * @return the builder
     */
    public Builder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    /**
     * With key name builder.
     *
     * @param keyName the key name
     * @return the builder
     */
    public Builder withKeyName(String keyName) {
      this.keyName = keyName;
      return this;
    }

    /**
     * With key builder.
     *
     * @param key the key
     * @return the builder
     */
    public Builder withKey(char[] key) {
      this.key = key;
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
     * With sudo app name builder.
     *
     * @param sudoAppName the sudo app name
     * @return the builder
     */
    public Builder withSudoAppName(String sudoAppName) {
      this.sudoAppName = sudoAppName;
      return this;
    }

    /**
     * With sudo app password builder.
     *
     * @param sudoAppPassword the sudo app password
     * @return the builder
     */
    public Builder withSudoAppPassword(char[] sudoAppPassword) {
      this.sudoAppPassword = sudoAppPassword;
      return this;
    }

    /**
     * With bastion host config builder.
     *
     * @param bastionHostConfig the bastion host config
     * @return the builder
     */
    public Builder withBastionHostConfig(SshSessionConfig bastionHostConfig) {
      this.bastionHostConfig = bastionHostConfig;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSshSessionConfig()
          .withAccountId(accountId)
          .withAppId(appId)
          .withExecutorType(executorType)
          .withExecutionId(executionId)
          .withCommandUnitName(commandUnitName)
          .withSocketConnectTimeout(socketConnectTimeout)
          .withSshConnectionTimeout(sshConnectionTimeout)
          .withSshSessionTimeout(sshSessionTimeout)
          .withRetryInterval(retryInterval)
          .withHost(host)
          .withPort(port)
          .withUserName(userName)
          .withPassword(password)
          .withKeyName(keyName)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withSudoAppName(sudoAppName)
          .withSudoAppPassword(sudoAppPassword)
          .withBastionHostConfig(bastionHostConfig);
    }

    /**
     * Build ssh session config.
     *
     * @return the ssh session config
     */
    public SshSessionConfig build() {
      SshSessionConfig sshSessionConfig = new SshSessionConfig();
      sshSessionConfig.setAccountId(accountId);
      sshSessionConfig.setAppId(appId);
      sshSessionConfig.setExecutorType(executorType);
      sshSessionConfig.setExecutionId(executionId);
      sshSessionConfig.setCommandUnitName(commandUnitName);
      sshSessionConfig.setSocketConnectTimeout(socketConnectTimeout);
      sshSessionConfig.setSshConnectionTimeout(sshConnectionTimeout);
      sshSessionConfig.setSshSessionTimeout(sshSessionTimeout);
      sshSessionConfig.setRetryInterval(retryInterval);
      sshSessionConfig.setHost(host);
      sshSessionConfig.setPort(port);
      sshSessionConfig.setUserName(userName);
      sshSessionConfig.setPassword(password);
      sshSessionConfig.setKeyName(keyName);
      sshSessionConfig.setKey(key);
      sshSessionConfig.setKeyPassphrase(keyPassphrase);
      sshSessionConfig.setSudoAppName(sudoAppName);
      sshSessionConfig.setSudoAppPassword(sudoAppPassword);
      sshSessionConfig.setBastionHostConfig(bastionHostConfig);
      return sshSessionConfig;
    }
  }
}
