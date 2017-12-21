package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesContainerInfo extends ContainerInfo {
  private String controllerType;
  private String controllerName;
  private String serviceName;
  private String podName;

  private KubernetesContainerInfo() {
    super();
  }

  public static final class Builder {
    private String clusterName;
    private String controllerType;
    private String controllerName;
    private String serviceName;
    private String podName;

    private Builder() {}

    public static Builder aKubernetesContainerInfo() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withControllerType(String controllerType) {
      this.controllerType = controllerType;
      return this;
    }

    public Builder withControllerName(String controllerName) {
      this.controllerName = controllerName;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withPodName(String podName) {
      this.podName = podName;
      return this;
    }

    public Builder but() {
      return aKubernetesContainerInfo()
          .withClusterName(clusterName)
          .withControllerType(controllerType)
          .withControllerName(controllerName)
          .withServiceName(serviceName)
          .withPodName(podName);
    }

    public KubernetesContainerInfo build() {
      KubernetesContainerInfo kubernetesContainerInfo = new KubernetesContainerInfo();
      kubernetesContainerInfo.setClusterName(clusterName);
      kubernetesContainerInfo.setControllerType(controllerType);
      kubernetesContainerInfo.setControllerName(controllerName);
      kubernetesContainerInfo.setServiceName(serviceName);
      kubernetesContainerInfo.setPodName(podName);
      return kubernetesContainerInfo;
    }
  }
}
