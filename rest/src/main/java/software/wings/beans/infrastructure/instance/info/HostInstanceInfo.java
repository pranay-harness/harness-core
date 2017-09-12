package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class HostInstanceInfo extends InstanceInfo {
  private String hostId;
  private String hostName;
  private String hostPublicDns;
}
