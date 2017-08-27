package software.wings.beans.infrastructure;

import com.amazonaws.services.ec2.model.Instance;

/**
 *
 * @author rktummala on 08/25/17
 */
public class Ec2InstanceMetadata extends InstanceMetadata {
  private Instance ec2Instance;

  public Instance getEc2Instance() {
    return ec2Instance;
  }

  public void setEc2Instance(Instance ec2Instance) {
    this.ec2Instance = ec2Instance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Ec2InstanceMetadata that = (Ec2InstanceMetadata) o;

    return ec2Instance != null ? ec2Instance.equals(that.ec2Instance) : that.ec2Instance == null;
  }

  @Override
  public int hashCode() {
    return ec2Instance != null ? ec2Instance.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Ec2InstanceMetaInfo{"
        + "ec2Instance=" + ec2Instance + '}';
  }

  public static final class Builder {
    private Instance ec2Instance;

    private Builder() {}

    public static Builder anEc2InstanceMetaInfo() {
      return new Builder();
    }

    public Builder withEc2Instance(Instance ec2Instance) {
      this.ec2Instance = ec2Instance;
      return this;
    }

    public Builder but() {
      return anEc2InstanceMetaInfo().withEc2Instance(ec2Instance);
    }

    public Ec2InstanceMetadata build() {
      Ec2InstanceMetadata ec2InstanceMetaInfo = new Ec2InstanceMetadata();
      ec2InstanceMetaInfo.setEc2Instance(ec2Instance);
      return ec2InstanceMetaInfo;
    }
  }
}
