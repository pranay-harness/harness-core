package software.wings.cloudprovider.aws;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.cloudprovider.ClusterConfiguration;

import java.util.List;

/**
 * Created by anubhaw on 12/29/16.
 */
@Data
@NoArgsConstructor
public class AwsClusterConfiguration extends ClusterConfiguration {
  private String serviceDefinition;
  private String launcherConfiguration;
  private String vpcZoneIdentifiers;
  private List<String> availabilityZones;
  private String autoScalingGroupName;

  @Builder
  public AwsClusterConfiguration(Integer size, String name, String serviceDefinition, String launcherConfiguration,
      String vpcZoneIdentifiers, List<String> availabilityZones, String autoScalingGroupName) {
    super(size, name);
    this.serviceDefinition = serviceDefinition;
    this.launcherConfiguration = launcherConfiguration;
    this.vpcZoneIdentifiers = vpcZoneIdentifiers;
    this.availabilityZones = availabilityZones;
    this.autoScalingGroupName = autoScalingGroupName;
  }
}
