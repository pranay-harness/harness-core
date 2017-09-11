package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesContainerInfo extends ContainerInfo {
  private String replicationControllerName;
  private String serviceName;
  private String podName;

  private KubernetesContainerInfo() {
    super();
  }

  public static final class Builder {
    private String replicationControllerName;
    private String podName;
    private String clusterName;
    private String serviceName;

    private Builder() {}

    public static Builder aKubernetesContainerInfo() {
      return new Builder();
    }

    public Builder withReplicationControllerName(String replicationControllerName) {
      this.replicationControllerName = replicationControllerName;
      return this;
    }

    public Builder withPodName(String podName) {
      this.podName = podName;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder but() {
      return aKubernetesContainerInfo()
          .withReplicationControllerName(replicationControllerName)
          .withPodName(podName)
          .withClusterName(clusterName)
          .withServiceName(serviceName);
    }

    public KubernetesContainerInfo build() {
      KubernetesContainerInfo kubernetesContainerInfo = new KubernetesContainerInfo();
      kubernetesContainerInfo.setReplicationControllerName(replicationControllerName);
      kubernetesContainerInfo.setPodName(podName);
      kubernetesContainerInfo.setClusterName(clusterName);
      kubernetesContainerInfo.setServiceName(serviceName);
      return kubernetesContainerInfo;
    }
  }
}
