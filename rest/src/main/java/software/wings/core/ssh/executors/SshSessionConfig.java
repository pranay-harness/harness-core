package software.wings.core.ssh.executors;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 2/8/16.
 */
@Data
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

  @SchemaIgnore private String encryptedPassword;

  @SchemaIgnore private String encryptedKey;

  @SchemaIgnore private String encryptedSudoAppPassword;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.SSH_SESSION_CONFIG;
  }

  @Override
  public List<Field> getEncryptedFields() {
    return WingsReflectionUtils.getEncryptedFields(this.getClass());
  }

  @Override
  public boolean isDecrypted() {
    return false;
  }

  @Override
  public void setDecrypted(boolean decrypted) {
    throw new IllegalStateException();
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
