package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 7/16/17.
 */
public class ServiceInstanceSelectionParams {
  private Integer count;
  private List<String> hostNames = new ArrayList<>();
  private List<String> excludedServiceInstanceIds = new ArrayList<>();
  private boolean selectSpecificHosts;

  /**
   * Gets count.
   *
   * @return the count
   */
  public Integer getCount() {
    return count;
  }

  /**
   * Sets count.
   *
   * @param count the count
   */
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   * Gets host names.
   *
   * @return the host names
   */
  public List<String> getHostNames() {
    return hostNames;
  }

  /**
   * Sets host names.
   *
   * @param hostNames the host names
   */
  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  /**
   * Gets excluded service instance ids.
   *
   * @return the excluded service instance ids
   */
  public List<String> getExcludedServiceInstanceIds() {
    return excludedServiceInstanceIds;
  }

  /**
   * Sets excluded service instance ids.
   *
   * @param excludedServiceInstanceIds the excluded service instance ids
   */
  public void setExcludedServiceInstanceIds(List<String> excludedServiceInstanceIds) {
    this.excludedServiceInstanceIds = excludedServiceInstanceIds;
  }

  /**
   * Is select specific hosts boolean.
   *
   * @return the boolean
   */
  public boolean isSelectSpecificHosts() {
    return selectSpecificHosts;
  }

  /**
   * Sets select specific hosts.
   *
   * @param selectSpecificHosts the select specific hosts
   */
  public void setSelectSpecificHosts(boolean selectSpecificHosts) {
    this.selectSpecificHosts = selectSpecificHosts;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Integer count;
    private List<String> hostNames = new ArrayList<>();
    private List<String> excludedServiceInstanceIds = new ArrayList<>();
    private boolean selectSpecificHosts;

    private Builder() {}

    /**
     * A service instance selection params builder.
     *
     * @return the builder
     */
    public static Builder aServiceInstanceSelectionParams() {
      return new Builder();
    }

    /**
     * With count builder.
     *
     * @param count the count
     * @return the builder
     */
    public Builder withCount(Integer count) {
      this.count = count;
      return this;
    }

    /**
     * With host names builder.
     *
     * @param hostNames the host names
     * @return the builder
     */
    public Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    /**
     * With excluded service instance ids builder.
     *
     * @param excludedServiceInstanceIds the excluded service instance ids
     * @return the builder
     */
    public Builder withExcludedServiceInstanceIds(List<String> excludedServiceInstanceIds) {
      this.excludedServiceInstanceIds = excludedServiceInstanceIds;
      return this;
    }

    /**
     * With select specific hosts builder.
     *
     * @param selectSpecificHosts the select specific hosts
     * @return the builder
     */
    public Builder withSelectSpecificHosts(boolean selectSpecificHosts) {
      this.selectSpecificHosts = selectSpecificHosts;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aServiceInstanceSelectionParams()
          .withCount(count)
          .withHostNames(hostNames)
          .withExcludedServiceInstanceIds(excludedServiceInstanceIds)
          .withSelectSpecificHosts(selectSpecificHosts);
    }

    /**
     * Build service instance selection params.
     *
     * @return the service instance selection params
     */
    public ServiceInstanceSelectionParams build() {
      ServiceInstanceSelectionParams serviceInstanceSelectionParams = new ServiceInstanceSelectionParams();
      serviceInstanceSelectionParams.setCount(count);
      serviceInstanceSelectionParams.setHostNames(hostNames);
      serviceInstanceSelectionParams.setExcludedServiceInstanceIds(excludedServiceInstanceIds);
      serviceInstanceSelectionParams.setSelectSpecificHosts(selectSpecificHosts);
      return serviceInstanceSelectionParams;
    }
  }
}
