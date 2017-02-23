package software.wings.cloudprovider.aws;

import software.wings.cloudprovider.ClusterConfiguration;

import java.util.List;

/**
 * Created by anubhaw on 12/29/16.
 */
public class AwsClusterConfiguration extends ClusterConfiguration {
  private String serviceDefinition;
  private String launcherConfiguration;
  private String vpcZoneIdentifiers;
  private List<String> availabilityZones;
  private String autoScalingGroupName;

  /**
   * Gets launcher configuration.
   *
   * @return the launcher configuration
   */
  public String getLauncherConfiguration() {
    return launcherConfiguration;
  }

  /**
   * Sets launcher configuration.
   *
   * @param launcherConfiguration the launcher configuration
   */
  public void setLauncherConfiguration(String launcherConfiguration) {
    this.launcherConfiguration = launcherConfiguration;
  }

  /**
   * Gets vpc zone identifiers.
   *
   * @return the vpc zone identifiers
   */
  public String getVpcZoneIdentifiers() {
    return vpcZoneIdentifiers;
  }

  /**
   * Sets vpc zone identifiers.
   *
   * @param vpcZoneIdentifiers the vpc zone identifiers
   */
  public void setVpcZoneIdentifiers(String vpcZoneIdentifiers) {
    this.vpcZoneIdentifiers = vpcZoneIdentifiers;
  }

  /**
   * Gets availability zones.
   *
   * @return the availability zones
   */
  public List<String> getAvailabilityZones() {
    return availabilityZones;
  }

  /**
   * Sets availability zones.
   *
   * @param availabilityZones the availability zones
   */
  public void setAvailabilityZones(List<String> availabilityZones) {
    this.availabilityZones = availabilityZones;
  }

  /**
   * Gets auto scaling group name.
   *
   * @return the auto scaling group name
   */
  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  /**
   * Sets auto scaling group name.
   *
   * @param autoScalingGroupName the auto scaling group name
   */
  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  /**
   * Gets service definition.
   *
   * @return the service definition
   */
  public String getServiceDefinition() {
    return serviceDefinition;
  }

  /**
   * Sets service definition.
   *
   * @param serviceDefinition the service definition
   */
  public void setServiceDefinition(String serviceDefinition) {
    this.serviceDefinition = serviceDefinition;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Integer size;
    private String name;
    private String serviceDefinition;
    private String launcherConfiguration;
    private String vpcZoneIdentifiers;
    private List<String> availabilityZones;
    private String autoScalingGroupName;

    private Builder() {}

    /**
     * An aws cluster configuration builder.
     *
     * @return the builder
     */
    public static Builder anAwsClusterConfiguration() {
      return new Builder();
    }

    /**
     * With size builder.
     *
     * @param size the size
     * @return the builder
     */
    public Builder withSize(Integer size) {
      this.size = size;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With service definition builder.
     *
     * @param serviceDefinition the service definition
     * @return the builder
     */
    public Builder withServiceDefinition(String serviceDefinition) {
      this.serviceDefinition = serviceDefinition;
      return this;
    }

    /**
     * With launcher configuration builder.
     *
     * @param launcherConfiguration the launcher configuration
     * @return the builder
     */
    public Builder withLauncherConfiguration(String launcherConfiguration) {
      this.launcherConfiguration = launcherConfiguration;
      return this;
    }

    /**
     * With vpc zone identifiers builder.
     *
     * @param vpcZoneIdentifiers the vpc zone identifiers
     * @return the builder
     */
    public Builder withVpcZoneIdentifiers(String vpcZoneIdentifiers) {
      this.vpcZoneIdentifiers = vpcZoneIdentifiers;
      return this;
    }

    /**
     * With availability zones builder.
     *
     * @param availabilityZones the availability zones
     * @return the builder
     */
    public Builder withAvailabilityZones(List<String> availabilityZones) {
      this.availabilityZones = availabilityZones;
      return this;
    }

    /**
     * With auto scaling group name builder.
     *
     * @param autoScalingGroupName the auto scaling group name
     * @return the builder
     */
    public Builder withAutoScalingGroupName(String autoScalingGroupName) {
      this.autoScalingGroupName = autoScalingGroupName;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsClusterConfiguration()
          .withSize(size)
          .withName(name)
          .withServiceDefinition(serviceDefinition)
          .withLauncherConfiguration(launcherConfiguration)
          .withVpcZoneIdentifiers(vpcZoneIdentifiers)
          .withAvailabilityZones(availabilityZones)
          .withAutoScalingGroupName(autoScalingGroupName);
    }

    /**
     * Build aws cluster configuration.
     *
     * @return the aws cluster configuration
     */
    public AwsClusterConfiguration build() {
      AwsClusterConfiguration awsClusterConfiguration = new AwsClusterConfiguration();
      awsClusterConfiguration.setSize(size);
      awsClusterConfiguration.setName(name);
      awsClusterConfiguration.setServiceDefinition(serviceDefinition);
      awsClusterConfiguration.setLauncherConfiguration(launcherConfiguration);
      awsClusterConfiguration.setVpcZoneIdentifiers(vpcZoneIdentifiers);
      awsClusterConfiguration.setAvailabilityZones(availabilityZones);
      awsClusterConfiguration.setAutoScalingGroupName(autoScalingGroupName);
      return awsClusterConfiguration;
    }
  }
}
