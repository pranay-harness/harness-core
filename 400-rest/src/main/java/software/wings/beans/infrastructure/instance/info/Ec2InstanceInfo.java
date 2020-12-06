package software.wings.beans.infrastructure.instance.info;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Ec2InstanceInfo extends AbstractEc2InstanceInfo {
  @Builder
  public Ec2InstanceInfo(String hostId, String hostName, String hostPublicDns, Instance ec2Instance) {
    super(hostId, hostName, hostPublicDns, ec2Instance);
  }
}
