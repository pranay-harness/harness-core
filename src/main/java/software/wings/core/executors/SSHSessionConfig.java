package software.wings.core.executors;

/**
 * Created by anubhaw on 2/8/16.
 */

// Fixme: Use builder pattern

public class SSHSessionConfig {
  private Integer SSHConnectionTimeout = 10000; // 10 seconds
  private Integer SSHSessionTimeout = 600000; // 10 minutes
  private Integer RetryInterval = 1000;

  private String host;
  private Integer port;
  private String user;
  private String password;
  private String key;
  private String sudoUserName;
  private String sudoUserPassword;

  public SSHSessionConfig(String host, Integer port, String user, String password) {
    this.host = host;
    this.port = port;
    this.user = user;
    this.password = password;
    this.key = password; // Fixme: with Builder pattern
  }

  public SSHSessionConfig(
      String host, Integer port, String user, String password, String sudoUserName, String sudoUserPassword) {
    this.host = host;
    this.port = port;
    this.user = user;
    this.password = password;
    this.sudoUserName = sudoUserName;
    this.sudoUserPassword = sudoUserPassword;
  }

  public Integer getSSHConnectionTimeout() {
    return SSHConnectionTimeout;
  }

  public void setSSHConnectionTimeout(Integer SSHConnectionTimeout) {
    this.SSHConnectionTimeout = SSHConnectionTimeout;
  }

  public Integer getSSHSessionTimeout() {
    return SSHSessionTimeout;
  }

  public void setSSHSessionTimeout(Integer SSHSessionTimeout) {
    this.SSHSessionTimeout = SSHSessionTimeout;
  }

  public Integer getRetryInterval() {
    return RetryInterval;
  }

  public void setRetryInterval(Integer retryInterval) {
    RetryInterval = retryInterval;
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

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
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
}
