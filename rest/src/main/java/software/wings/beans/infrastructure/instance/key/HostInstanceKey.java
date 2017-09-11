package software.wings.beans.infrastructure.instance.key;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Indexed;

/**
 * Host based instance key like physical host and cloud instances like ec2 , gcp instance.
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HostInstanceKey extends InstanceKey {
  @Indexed private String hostName;
  @Indexed private String infraMappingId;

  public static final class Builder {
    private String infraMappingId;
    private String hostName;

    private Builder() {}

    public static Builder aHostInstanceKey() {
      return new Builder();
    }

    public Builder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder but() {
      return aHostInstanceKey().withInfraMappingId(infraMappingId).withHostName(hostName);
    }

    public HostInstanceKey build() {
      HostInstanceKey hostInstanceKey = new HostInstanceKey();
      hostInstanceKey.setInfraMappingId(infraMappingId);
      hostInstanceKey.setHostName(hostName);
      return hostInstanceKey;
    }
  }
}
