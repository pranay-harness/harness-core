package software.wings.beans.infrastructure.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PhysicalHostInstanceInfo extends HostInstanceInfo {
  @Builder
  public PhysicalHostInstanceInfo(String hostId, String hostName, String hostPublicDns) {
    super(hostId, hostName, hostPublicDns);
  }
}
