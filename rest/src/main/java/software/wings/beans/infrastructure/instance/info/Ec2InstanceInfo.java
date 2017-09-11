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
public class Ec2InstanceInfo extends HostInstanceInfo {
  private Instance ec2Instance;

  public static final class Builder {
    private Instance ec2Instance;
    private String hostId;
    private String hostName;
    private String hostPublicDns;

    private Builder() {}

    public static Builder anEc2InstanceInfo() {
      return new Builder();
    }

    public Builder withEc2Instance(Instance ec2Instance) {
      this.ec2Instance = ec2Instance;
      return this;
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
      return anEc2InstanceInfo()
          .withEc2Instance(ec2Instance)
          .withHostId(hostId)
          .withHostName(hostName)
          .withHostPublicDns(hostPublicDns);
    }

    public Ec2InstanceInfo build() {
      Ec2InstanceInfo ec2InstanceInfo = new Ec2InstanceInfo();
      ec2InstanceInfo.setEc2Instance(ec2Instance);
      ec2InstanceInfo.setHostId(hostId);
      ec2InstanceInfo.setHostName(hostName);
      ec2InstanceInfo.setHostPublicDns(hostPublicDns);
      return ec2InstanceInfo;
    }
  }
}
