package software.wings.graphql.schema.type.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class QLHostInstanceInfo extends QLInstanceInfo {
  private String hostId;
  private String hostName;
  private String hostPublicDns;

  public QLHostInstanceInfo(String hostId, String hostName, String hostPublicDns) {
    this.hostId = hostId;
    this.hostName = hostName;
    this.hostPublicDns = hostPublicDns;
  }
}
