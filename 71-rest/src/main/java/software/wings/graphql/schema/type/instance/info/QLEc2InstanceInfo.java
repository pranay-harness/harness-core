package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QLEc2InstanceInfo extends QLAbstractEc2InstanceInfo {
  private String autoScalingGroupName;
  private String deploymentId;

  @Builder
  public QLEc2InstanceInfo(
      String hostId, String hostName, String hostPublicDns, String autoScalingGroupName, String deploymentId) {
    super(hostId, hostName, hostPublicDns);
    this.deploymentId = deploymentId;
    this.autoScalingGroupName = autoScalingGroupName;
  }
}
