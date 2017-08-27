package software.wings.beans.stats.dashboard;

import software.wings.beans.KeyValuePair;

import java.util.List;

/**
 * Instance information as key value pairs
 * @author rktummala on 08/25/17
 */
public class InstanceDetails {
  private List<KeyValuePair> instanceInfo;
  private List<KeyValuePair> deploymentInfo;

  public List<KeyValuePair> getInstanceInfo() {
    return instanceInfo;
  }

  public void setInstanceInfo(List<KeyValuePair> instanceInfo) {
    this.instanceInfo = instanceInfo;
  }

  public List<KeyValuePair> getDeploymentInfo() {
    return deploymentInfo;
  }

  public void setDeploymentInfo(List<KeyValuePair> deploymentInfo) {
    this.deploymentInfo = deploymentInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    InstanceDetails that = (InstanceDetails) o;

    if (instanceInfo != null ? !instanceInfo.equals(that.instanceInfo) : that.instanceInfo != null)
      return false;
    return deploymentInfo != null ? deploymentInfo.equals(that.deploymentInfo) : that.deploymentInfo == null;
  }

  @Override
  public int hashCode() {
    int result = instanceInfo != null ? instanceInfo.hashCode() : 0;
    result = 31 * result + (deploymentInfo != null ? deploymentInfo.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "InstanceDetails{"
        + "instanceInfo=" + instanceInfo + ", deploymentInfo=" + deploymentInfo + '}';
  }

  public static final class Builder {
    private List<KeyValuePair> instanceInfo;
    private List<KeyValuePair> deploymentInfo;

    private Builder() {}

    public static Builder anInstanceDetails() {
      return new Builder();
    }

    public Builder withInstanceInfo(List<KeyValuePair> instanceInfo) {
      this.instanceInfo = instanceInfo;
      return this;
    }

    public Builder withDeploymentInfo(List<KeyValuePair> deploymentInfo) {
      this.deploymentInfo = deploymentInfo;
      return this;
    }

    public InstanceDetails build() {
      InstanceDetails instanceDetails = new InstanceDetails();
      instanceDetails.setInstanceInfo(instanceInfo);
      instanceDetails.setDeploymentInfo(deploymentInfo);
      return instanceDetails;
    }
  }
}