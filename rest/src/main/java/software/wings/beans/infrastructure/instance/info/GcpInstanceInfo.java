package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GcpInstanceInfo extends HostInstanceInfo {
  public static final class Builder {
    private String hostId;
    private String hostName;
    private String hostPublicDns;

    private Builder() {}

    public static Builder aGcpInstanceInfo() {
      return new Builder();
    }

    public Builder withHostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withHostPublicDns(String hostPublicDns) {
      this.hostPublicDns = hostPublicDns;
      return this;
    }

    public Builder but() {
      return aGcpInstanceInfo().withHostId(hostId).withHostName(hostName).withHostPublicDns(hostPublicDns);
    }

    public GcpInstanceInfo build() {
      GcpInstanceInfo gcpInstanceInfo = new GcpInstanceInfo();
      gcpInstanceInfo.setHostId(hostId);
      gcpInstanceInfo.setHostName(hostName);
      gcpInstanceInfo.setHostPublicDns(hostPublicDns);
      return gcpInstanceInfo;
    }
  }
}
