package software.wings.delegate.app;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateConfiguration {
  private String managerUrl;

  private String accountId;

  private String accountSecret;

  private long heartbeatIntervalMs;

  private String localDiskPath;

  private boolean doUpgrade;

  /**
   * Getter for property 'managerUrl'.
   *
   * @return Value for property 'managerUrl'.
   */
  public String getManagerUrl() {
    return managerUrl;
  }

  /**
   * Setter for property 'managerUrl'.
   *
   * @param managerUrl Value to set for property 'managerUrl'.
   */
  public void setManagerUrl(String managerUrl) {
    this.managerUrl = managerUrl;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAccountSecret() {
    return accountSecret;
  }

  public void setAccountSecret(String accountSecret) {
    this.accountSecret = accountSecret;
  }

  public long getHeartbeatIntervalMs() {
    return heartbeatIntervalMs;
  }

  public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
    this.heartbeatIntervalMs = heartbeatIntervalMs;
  }

  public String getLocalDiskPath() {
    return localDiskPath;
  }

  public void setLocalDiskPath(String localDiskPath) {
    this.localDiskPath = localDiskPath;
  }

  public boolean isDoUpgrade() {
    return doUpgrade;
  }

  public void setDoUpgrade(boolean doUpgrade) {
    this.doUpgrade = doUpgrade;
  }
}
