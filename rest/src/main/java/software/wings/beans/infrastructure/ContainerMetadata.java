package software.wings.beans.infrastructure;

/**
 * Base class for container instance like docker
 * @author rktummala on 08/25/17
 */
public class ContainerMetadata extends InstanceMetadata {
  private String clusterName;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ContainerMetadata that = (ContainerMetadata) o;

    return clusterName != null ? clusterName.equals(that.clusterName) : that.clusterName == null;
  }

  @Override
  public int hashCode() {
    return clusterName != null ? clusterName.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ContainerMetadata{"
        + "clusterName='" + clusterName + '\'' + '}';
  }

  public static final class Builder {
    private String clusterName;

    private Builder() {}

    public static Builder aContainerMetadata() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder but() {
      return aContainerMetadata().withClusterName(clusterName);
    }

    public ContainerMetadata build() {
      ContainerMetadata containerMetadata = new ContainerMetadata();
      containerMetadata.setClusterName(clusterName);
      return containerMetadata;
    }
  }
}
