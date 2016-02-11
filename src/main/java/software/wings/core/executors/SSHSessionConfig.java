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
  private String key;
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
    this.key = builder.key;
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
    private String key;
    private String sudoUserName;
    private String sudoUserPassword;

    public SSHSessionConfigBuilder setSSHConnectionTimeout(Integer sshConnectionTimeout) {
      this.SSHConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    public SSHSessionConfigBuilder setSSHSessionTimeout(Integer sshSessionTimeout) {
      this.SSHSessionTimeout = sshSessionTimeout;
      return this;
    }

    public SSHSessionConfigBuilder setRetryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public SSHSessionConfigBuilder setHost(String host) {
      this.host = host;
      return this;
    }

    public SSHSessionConfigBuilder setPort(Integer port) {
      this.port = port;
      return this;
    }

    public SSHSessionConfigBuilder setUser(String user) {
      this.user = user;
      return this;
    }

    public SSHSessionConfigBuilder setPassword(String password) {
      this.password = password;
      return this;
    }

    public SSHSessionConfigBuilder setKey(String key) {
      this.key = key;
      return this;
    }

    public SSHSessionConfigBuilder setSudoUserName(String sudoUserName) {
      this.sudoUserName = sudoUserName;
      return this;
    }

    public SSHSessionConfigBuilder setSudoUserPassword(String sudoUserPassword) {
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

  public String getKey() {
    return key;
  }

  public String getSudoUserName() {
    return sudoUserName;
  }

  public String getSudoUserPassword() {
    return sudoUserPassword;
  }
}