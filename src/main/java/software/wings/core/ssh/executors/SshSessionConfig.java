package software.wings.core.ssh.executors;

import com.google.common.base.MoreObjects;

import software.wings.core.ssh.executors.SshExecutor.ExecutorType;

import java.util.Objects;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshSessionConfig {
  private ExecutorType executorType;
  private String executionId;
  private Integer sshConnectionTimeout = 5 * 60 * 1000; // 5 secs
  private Integer sshSessionTimeout = 10 * 60 * 1000; // 10 minutes
  private Integer retryInterval;
  private String host;
  private Integer port;
  private String userName;
  private String password;
  private String key;
  private String keyPassphrase;
  private String sudoUserName;
  private String sudoUserPassword;
  private SshSessionConfig jumpboxConfig;

  public ExecutorType getExecutorType() {
    return executorType;
  }

  public void setExecutorType(ExecutorType executorType) {
    this.executorType = executorType;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public Integer getSshConnectionTimeout() {
    return sshConnectionTimeout;
  }

  public void setSshConnectionTimeout(Integer sshConnectionTimeout) {
    this.sshConnectionTimeout = sshConnectionTimeout;
  }

  public Integer getSshSessionTimeout() {
    return sshSessionTimeout;
  }

  public void setSshSessionTimeout(Integer sshSessionTimeout) {
    this.sshSessionTimeout = sshSessionTimeout;
  }

  public Integer getRetryInterval() {
    return retryInterval;
  }

  public void setRetryInterval(Integer retryInterval) {
    this.retryInterval = retryInterval;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getKeyPassphrase() {
    return keyPassphrase;
  }

  public void setKeyPassphrase(String keyPassphrase) {
    this.keyPassphrase = keyPassphrase;
  }

  public String getSudoUserName() {
    return sudoUserName;
  }

  public void setSudoUserName(String sudoUserName) {
    this.sudoUserName = sudoUserName;
  }

  public String getSudoUserPassword() {
    return sudoUserPassword;
  }

  public void setSudoUserPassword(String sudoUserPassword) {
    this.sudoUserPassword = sudoUserPassword;
  }

  public SshSessionConfig getJumpboxConfig() {
    return jumpboxConfig;
  }

  public void setJumpboxConfig(SshSessionConfig jumpboxConfig) {
    this.jumpboxConfig = jumpboxConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(executorType, executionId, sshConnectionTimeout, sshSessionTimeout, retryInterval, host, port,
        userName, password, key, keyPassphrase, sudoUserName, sudoUserPassword, jumpboxConfig);
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
    return Objects.equals(this.executorType, other.executorType) && Objects.equals(this.executionId, other.executionId)
        && Objects.equals(this.sshConnectionTimeout, other.sshConnectionTimeout)
        && Objects.equals(this.sshSessionTimeout, other.sshSessionTimeout)
        && Objects.equals(this.retryInterval, other.retryInterval) && Objects.equals(this.host, other.host)
        && Objects.equals(this.port, other.port) && Objects.equals(this.userName, other.userName)
        && Objects.equals(this.password, other.password) && Objects.equals(this.key, other.key)
        && Objects.equals(this.keyPassphrase, other.keyPassphrase)
        && Objects.equals(this.sudoUserName, other.sudoUserName)
        && Objects.equals(this.sudoUserPassword, other.sudoUserPassword)
        && Objects.equals(this.jumpboxConfig, other.jumpboxConfig);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("executorType", executorType)
        .add("executionId", executionId)
        .add("sshConnectionTimeout", sshConnectionTimeout)
        .add("sshSessionTimeout", sshSessionTimeout)
        .add("retryInterval", retryInterval)
        .add("host", host)
        .add("port", port)
        .add("userName", userName)
        .add("password", password)
        .add("key", key)
        .add("keyPassphrase", keyPassphrase)
        .add("sudoUserName", sudoUserName)
        .add("sudoUserPassword", sudoUserPassword)
        .add("jumpboxConfig", jumpboxConfig)
        .toString();
  }

  public static final class SshSessionConfigBuilder {
    private ExecutorType executorType;
    private String executionId;
    private Integer sshConnectionTimeout = 5 * 60 * 1000; // 5 secs
    private Integer sshSessionTimeout = 10 * 60 * 1000; // 10 minutes
    private Integer retryInterval;
    private String host;
    private Integer port;
    private String userName;
    private String password;
    private String key;
    private String keyPassphrase;
    private String sudoUserName;
    private String sudoUserPassword;
    private SshSessionConfig jumpboxConfig;

    private SshSessionConfigBuilder() {}

    public static SshSessionConfigBuilder aSshSessionConfig() {
      return new SshSessionConfigBuilder();
    }

    public SshSessionConfigBuilder withExecutorType(ExecutorType executorType) {
      this.executorType = executorType;
      return this;
    }

    public SshSessionConfigBuilder withExecutionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    public SshSessionConfigBuilder withSshConnectionTimeout(Integer sshConnectionTimeout) {
      this.sshConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    public SshSessionConfigBuilder withSshSessionTimeout(Integer sshSessionTimeout) {
      this.sshSessionTimeout = sshSessionTimeout;
      return this;
    }

    public SshSessionConfigBuilder withRetryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public SshSessionConfigBuilder withHost(String host) {
      this.host = host;
      return this;
    }

    public SshSessionConfigBuilder withPort(Integer port) {
      this.port = port;
      return this;
    }

    public SshSessionConfigBuilder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    public SshSessionConfigBuilder withPassword(String password) {
      this.password = password;
      return this;
    }

    public SshSessionConfigBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    public SshSessionConfigBuilder withKeyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    public SshSessionConfigBuilder withSudoUserName(String sudoUserName) {
      this.sudoUserName = sudoUserName;
      return this;
    }

    public SshSessionConfigBuilder withSudoUserPassword(String sudoUserPassword) {
      this.sudoUserPassword = sudoUserPassword;
      return this;
    }

    public SshSessionConfigBuilder withJumpboxConfig(SshSessionConfig jumpboxConfig) {
      this.jumpboxConfig = jumpboxConfig;
      return this;
    }

    public SshSessionConfigBuilder but() {
      return aSshSessionConfig()
          .withExecutorType(executorType)
          .withExecutionId(executionId)
          .withSshConnectionTimeout(sshConnectionTimeout)
          .withSshSessionTimeout(sshSessionTimeout)
          .withRetryInterval(retryInterval)
          .withHost(host)
          .withPort(port)
          .withUserName(userName)
          .withPassword(password)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withSudoUserName(sudoUserName)
          .withSudoUserPassword(sudoUserPassword)
          .withJumpboxConfig(jumpboxConfig);
    }

    public SshSessionConfig build() {
      SshSessionConfig sshSessionConfig = new SshSessionConfig();
      sshSessionConfig.setExecutorType(executorType);
      sshSessionConfig.setExecutionId(executionId);
      sshSessionConfig.setSshConnectionTimeout(sshConnectionTimeout);
      sshSessionConfig.setSshSessionTimeout(sshSessionTimeout);
      sshSessionConfig.setRetryInterval(retryInterval);
      sshSessionConfig.setHost(host);
      sshSessionConfig.setPort(port);
      sshSessionConfig.setUserName(userName);
      sshSessionConfig.setPassword(password);
      sshSessionConfig.setKey(key);
      sshSessionConfig.setKeyPassphrase(keyPassphrase);
      sshSessionConfig.setSudoUserName(sudoUserName);
      sshSessionConfig.setSudoUserPassword(sudoUserPassword);
      sshSessionConfig.setJumpboxConfig(jumpboxConfig);
      return sshSessionConfig;
    }
  }
}
