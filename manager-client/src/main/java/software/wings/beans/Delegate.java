package software.wings.beans;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Delegate {
  private String uuid;
  private String accountId;
  private Status status;
  private String ip;
  private String hostName;
  private long lastHeartBeat;
  private String version;

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Getter for property 'status'.
   *
   * @return Value for property 'status'.
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Setter for property 'status'.
   *
   * @param status Value to set for property 'status'.
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Getter for property 'ip'.
   *
   * @return Value for property 'ip'.
   */
  public String getIp() {
    return ip;
  }

  /**
   * Setter for property 'ip'.
   *
   * @param ip Value to set for property 'ip'.
   */
  public void setIp(String ip) {
    this.ip = ip;
  }

  /**
   * Getter for property 'hostName'.
   *
   * @return Value for property 'hostName'.
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Setter for property 'hostName'.
   *
   * @param hostName Value to set for property 'hostName'.
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Getter for property 'lastHeartBeat'.
   *
   * @return Value for property 'lastHeartBeat'.
   */
  public long getLastHeartBeat() {
    return lastHeartBeat;
  }

  /**
   * Setter for property 'lastHeartBeat'.
   *
   * @param lastHeartBeat Value to set for property 'lastHeartBeat'.
   */
  public void setLastHeartBeat(long lastHeartBeat) {
    this.lastHeartBeat = lastHeartBeat;
  }

  /**
   * Getter for property 'uuid'.
   *
   * @return Value for property 'uuid'.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Setter for property 'uuid'.
   *
   * @param uuid Value to set for property 'uuid'.
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Getter for property 'version'.
   *
   * @return Value for property 'version'.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Setter for property 'version'.
   *
   * @param version Value to set for property 'version'.
   */
  public void setVersion(String version) {
    this.version = version;
  }

  public enum Status { ENABLED, DISABLED, DISCONNECTED, UPGRADING }

  public static final class Builder {
    private String uuid;
    private String accountId;
    private Status status;
    private String ip;
    private String hostName;
    private long lastHeartBeat;
    private String version;

    private Builder() {}

    public static Builder aDelegate() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withIp(String ip) {
      this.ip = ip;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withLastHeartBeat(long lastHeartBeat) {
      this.lastHeartBeat = lastHeartBeat;
      return this;
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder but() {
      return aDelegate()
          .withUuid(uuid)
          .withAccountId(accountId)
          .withStatus(status)
          .withIp(ip)
          .withHostName(hostName)
          .withLastHeartBeat(lastHeartBeat)
          .withVersion(version);
    }

    public Delegate build() {
      Delegate delegate = new Delegate();
      delegate.setUuid(uuid);
      delegate.setAccountId(accountId);
      delegate.setStatus(status);
      delegate.setIp(ip);
      delegate.setHostName(hostName);
      delegate.setLastHeartBeat(lastHeartBeat);
      delegate.setVersion(version);
      return delegate;
    }
  }
}
