package software.wings.beans;

public class AzureKubernetesCluster {
  private String name;
  private String resourceGroup;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getResourceGroup() {
    return resourceGroup;
  }

  public void setResourceGroup(String resourceGroup) {
    this.resourceGroup = resourceGroup;
  }

  public static final class Builder {
    private String name;
    private String resourceGroup;

    private Builder() {}

    public static Builder anAzureKubernetesCluster() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withResourceGroup(String resourceGroup) {
      this.resourceGroup = resourceGroup;
      return this;
    }

    public AzureKubernetesCluster build() {
      AzureKubernetesCluster cluster = new AzureKubernetesCluster();
      cluster.setName(name);
      cluster.setResourceGroup(resourceGroup);
      return cluster;
    }
  }
}
