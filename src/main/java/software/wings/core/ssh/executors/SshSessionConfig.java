package software.wings.core.ssh.executors;

import software.wings.core.ssh.executors.SshExecutor.ExecutorType;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshSessionConfig {
  private ExecutorType executorType;
  private String executionID;
  private Integer SSHConnectionTimeout;
  private Integer SSHSessionTimeout;
  private Integer retryInterval;
  private String host;
  private Integer port;
  private String user;
  private String password;
  private String keyPath;
  private String keyPassphrase;
  private String sudoUserName;
  private String sudoUserPassword;
  private SshSessionConfig jumpboxConfig;

  public SshSessionConfig(SshSessionConfigBuilder builder) {
    this.executorType = builder.executorType;
    this.executionID = builder.executionID;
    this.SSHConnectionTimeout = builder.SSHConnectionTimeout;
    this.SSHSessionTimeout = builder.SSHSessionTimeout;
    retryInterval = builder.retryInterval;
    this.host = builder.host;
    this.port = builder.port;
    this.user = builder.user;
    this.password = builder.password;
    this.keyPath = builder.keyPath;
    this.keyPassphrase = builder.keyPassphrase;
    this.sudoUserName = builder.sudoUserName;
    this.sudoUserPassword = builder.sudoUserPassword;
    this.jumpboxConfig = builder.jumpboxConfig;
  }

  public Integer getSSHConnectionTimeout() {
    return SSHConnectionTimeout;
  }

  public Integer getSSHSessionTimeout() {
    return SSHSessionTimeout;
  }

  public Integer getRetryInterval() {
    return retryInterval;
  }

  public ExecutorType getExecutorType() {
    return executorType;
  }

  public String getExecutionID() {
    return executionID;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getKeyPassphrase() {
    return keyPassphrase;
  }

  public String getKeyPath() {
    return keyPath;
  }

  public String getSudoUserName() {
    return sudoUserName;
  }

  public String getSudoUserPassword() {
    return sudoUserPassword;
  }

  public SshSessionConfig getJumpboxConfig() {
    return jumpboxConfig;
  }

  public static class SshSessionConfigBuilder {
    public String executionID;
    private Integer SSHConnectionTimeout = 10000; // 10 seconds
    private Integer retryInterval = 1000;
    private ExecutorType executorType;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String keyPath;
    private String keyPassphrase;
    private Integer SSHSessionTimeout = 10000; // 10 minutes
    private String sudoUserName;
    private String sudoUserPassword;
    private SshSessionConfig jumpboxConfig;

    public SshSessionConfigBuilder SSHConnectionTimeout(Integer sshConnectionTimeout) {
      this.SSHConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    public SshSessionConfigBuilder SSHSessionTimeout(Integer sshSessionTimeout) {
      this.SSHSessionTimeout = sshSessionTimeout;
      return this;
    }

    public SshSessionConfigBuilder retryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public SshSessionConfigBuilder executionType(ExecutorType executorType) {
      this.executorType = executorType;
      return this;
    }

    public SshSessionConfigBuilder executionID(String executionID) {
      this.executionID = executionID;
      return this;
    }

    public SshSessionConfigBuilder host(String host) {
      this.host = host;
      return this;
    }

    public SshSessionConfigBuilder port(Integer port) {
      this.port = port;
      return this;
    }

    public SshSessionConfigBuilder user(String user) {
      this.user = user;
      return this;
    }

    public SshSessionConfigBuilder password(String password) {
      this.password = password;
      return this;
    }

    public SshSessionConfigBuilder keyPath(String key) {
      this.keyPath = key;
      return this;
    }

    public SshSessionConfigBuilder keyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    public SshSessionConfigBuilder sudoUserName(String sudoUserName) {
      this.sudoUserName = sudoUserName;
      return this;
    }

    public SshSessionConfigBuilder sudoUserPassword(String sudoUserPassword) {
      this.sudoUserPassword = sudoUserPassword;
      return this;
    }

    public SshSessionConfigBuilder jumpboxConfig(SshSessionConfig jumpboxConfig) {
      this.jumpboxConfig = jumpboxConfig;
      return this;
    }

    public SshSessionConfig build() {
      return new SshSessionConfig(this);
    }
  }
}
