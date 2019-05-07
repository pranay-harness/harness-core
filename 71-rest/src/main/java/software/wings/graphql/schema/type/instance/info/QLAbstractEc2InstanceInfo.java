package software.wings.graphql.schema.type.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class QLAbstractEc2InstanceInfo extends QLHostInstanceInfo {
  public QLAbstractEc2InstanceInfo(String hostId, String hostName, String hostPublicDns) {
    super(hostId, hostName, hostPublicDns);
  }
}
