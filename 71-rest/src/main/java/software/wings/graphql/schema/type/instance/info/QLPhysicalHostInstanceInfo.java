package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QLPhysicalHostInstanceInfo extends QLHostInstanceInfo {
  @Builder
  public QLPhysicalHostInstanceInfo(String hostId, String hostName, String hostPublicDns) {
    super(hostId, hostName, hostPublicDns);
  }
}
