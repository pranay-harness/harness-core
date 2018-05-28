package software.wings.core.ssh.executors;

import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  private String keyPath;

  private boolean keyLess;

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
    private String keyPath;
    private boolean keyLess;

    private Builder() {}

    public static Builder aSshSessionConfig() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withExecutorType(ExecutorType executorType) {
      this.executorType = executorType;
      return this;
    }

    public Builder withExecutionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    public Builder withCommandUnitName(String commandUnitName) {
      this.commandUnitName = commandUnitName;
      return this;
    }

    public Builder withSocketConnectTimeout(Integer socketConnectTimeout) {
      this.socketConnectTimeout = socketConnectTimeout;
      return this;
    }

    public Builder withSshConnectionTimeout(Integer sshConnectionTimeout) {
      this.sshConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    public Builder withSshSessionTimeout(Integer sshSessionTimeout) {
      this.sshSessionTimeout = sshSessionTimeout;
      return this;
    }

    public Builder withRetryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public Builder withHost(String host) {
      this.host = host;
      return this;
    }

    public Builder withPort(Integer port) {
      this.port = port;
      return this;
    }

    public Builder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    public Builder withKeyName(String keyName) {
      this.keyName = keyName;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withKey(char[] key) {
      this.key = key;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withKeyPassphrase(char[] keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    public Builder withSudoAppName(String sudoAppName) {
      this.sudoAppName = sudoAppName;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withSudoAppPassword(char[] sudoAppPassword) {
      this.sudoAppPassword = sudoAppPassword;
      return this;
    }

    public Builder withBastionHostConfig(SshSessionConfig bastionHostConfig) {
      this.bastionHostConfig = bastionHostConfig;
      return this;
    }

    public Builder withKeyPath(String keyPath) {
      this.keyPath = keyPath;
      return this;
    }

    public Builder withKeyLess(boolean keyLess) {
      this.keyLess = keyLess;
      return this;
    }

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
          .withBastionHostConfig(bastionHostConfig)
          .withKeyPath(keyPath)
          .withKeyLess(keyLess);
    }

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
      sshSessionConfig.setKeyPath(keyPath);
      sshSessionConfig.setKeyLess(keyLess);
      return sshSessionConfig;
    }
  }
}
