package software.wings.beans.infrastructure.instance.info;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 01/30/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeDeployInstanceInfo extends AbstractEc2InstanceInfo {
  private String deploymentId;

  @Builder
  public CodeDeployInstanceInfo(
      String hostId, String hostName, String hostPublicDns, Instance ec2Instance, String deploymentId) {
    super(hostId, hostName, hostPublicDns, ec2Instance);
    this.deploymentId = deploymentId;
  }
}
