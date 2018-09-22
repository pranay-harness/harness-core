package software.wings.beans.infrastructure.instance.info;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractEc2InstanceInfo extends HostInstanceInfo {
  private Instance ec2Instance;

  public AbstractEc2InstanceInfo(String hostId, String hostName, String hostPublicDns, Instance ec2Instance) {
    super(hostId, hostName, hostPublicDns);
    this.ec2Instance = ec2Instance;
  }
}
