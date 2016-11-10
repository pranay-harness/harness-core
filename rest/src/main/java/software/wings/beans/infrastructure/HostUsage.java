package software.wings.beans.infrastructure;

import java.util.List;

/**
 * Created by anubhaw on 9/16/16.
 */
public class HostUsage {
  private int totalCount;
  private int unmappedHostCount;
  private List<ApplicationHostUsage> applicationHosts;

  /**
   * Gets total count.
   *
   * @return the total count
   */
  public int getTotalCount() {
    return totalCount;
  }

  /**
   * Sets total count.
   *
   * @param totalCount the total count
   */
  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  /**
   * Gets application hosts.
   *
   * @return the application hosts
   */
  public List<ApplicationHostUsage> getApplicationHosts() {
    return applicationHosts;
  }

  /**
   * Sets application hosts.
   *
   * @param applicationHosts the application hosts
   */
  public void setApplicationHosts(List<ApplicationHostUsage> applicationHosts) {
    this.applicationHosts = applicationHosts;
  }

  /**
   * Gets unmapped host count.
   *
   * @return the unmapped host count
   */
  public int getUnmappedHostCount() {
    return unmappedHostCount;
  }

  /**
   * Sets unmapped host count.
   *
   * @param unmappedHostCount the unmapped host count
   */
  public void setUnmappedHostCount(int unmappedHostCount) {
    this.unmappedHostCount = unmappedHostCount;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int totalCount;
    private int unmappedHostCount;
    private List<ApplicationHostUsage> applicationHosts;

    private Builder() {}

    /**
     * A host usage builder.
     *
     * @return the builder
     */
    public static Builder aHostUsage() {
      return new Builder();
    }

    /**
     * With total count builder.
     *
     * @param totalCount the total count
     * @return the builder
     */
    public Builder withTotalCount(int totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    /**
     * With unmapped host count builder.
     *
     * @param unmappedHostCount the unmapped host count
     * @return the builder
     */
    public Builder withUnmappedHostCount(int unmappedHostCount) {
      this.unmappedHostCount = unmappedHostCount;
      return this;
    }

    /**
     * With application hosts builder.
     *
     * @param applicationHosts the application hosts
     * @return the builder
     */
    public Builder withApplicationHosts(List<ApplicationHostUsage> applicationHosts) {
      this.applicationHosts = applicationHosts;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHostUsage()
          .withTotalCount(totalCount)
          .withUnmappedHostCount(unmappedHostCount)
          .withApplicationHosts(applicationHosts);
    }

    /**
     * Build host usage.
     *
     * @return the host usage
     */
    public HostUsage build() {
      HostUsage hostUsage = new HostUsage();
      hostUsage.setTotalCount(totalCount);
      hostUsage.setUnmappedHostCount(unmappedHostCount);
      hostUsage.setApplicationHosts(applicationHosts);
      return hostUsage;
    }
  }
}
