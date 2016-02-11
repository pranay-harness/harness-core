package software.wings.core.executors;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHSessionConfig {
  private Integer SSHConnectionTimeout;
  private Integer SSHSessionTimeout;
  private Integer retryInterval;
  private String host;
  private Integer port;
  private String user;
  private String password;
  private String keyPath;
  private String sudoUserName;
  private String sudoUserPassword;

  public SSHSessionConfig(SSHSessionConfigBuilder builder) {
    this.SSHConnectionTimeout = builder.SSHConnectionTimeout;
    this.SSHSessionTimeout = builder.SSHSessionTimeout;
    retryInterval = builder.retryInterval;
    this.host = builder.host;
    this.port = builder.port;
    this.user = builder.user;
    this.password = builder.password;
    this.keyPath = builder.keyPath;
    this.sudoUserName = builder.sudoUserName;
    this.sudoUserPassword = builder.sudoUserPassword;
  }

  public static class SSHSessionConfigBuilder {
    private Integer SSHConnectionTimeout = 10000; // 10 seconds
    private Integer SSHSessionTimeout = 600000; // 10 minutes
    private Integer retryInterval = 1000;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String keyPath;
    private String sudoUserName;
    private String sudoUserPassword;

    public SSHSessionConfigBuilder SSHConnectionTimeout(Integer sshConnectionTimeout) {
      this.SSHConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    public SSHSessionConfigBuilder SSHSessionTimeout(Integer sshSessionTimeout) {
      this.SSHSessionTimeout = sshSessionTimeout;
      return this;
    }

    public SSHSessionConfigBuilder retryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public SSHSessionConfigBuilder host(String host) {
      this.host = host;
      return this;
    }

    public SSHSessionConfigBuilder port(Integer port) {
      this.port = port;
      return this;
    }

    public SSHSessionConfigBuilder user(String user) {
      this.user = user;
      return this;
    }

    public SSHSessionConfigBuilder password(String password) {
      this.password = password;
      return this;
    }

    public SSHSessionConfigBuilder keyPath(String key) {
      this.keyPath = key;
      return this;
    }

    public SSHSessionConfigBuilder sudoUserName(String sudoUserName) {
      this.sudoUserName = sudoUserName;
      return this;
    }

    public SSHSessionConfigBuilder sudoUserPassword(String sudoUserPassword) {
      this.sudoUserPassword = sudoUserPassword;
      return this;
    }

    public SSHSessionConfig build() {
      return new SSHSessionConfig(this);
    }
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

  public String getKeyPath() {
    return keyPath;
  }

  public String getSudoUserName() {
    return sudoUserName;
  }

  public String getSudoUserPassword() {
    return sudoUserPassword;
  }
}