package software.wings.beans.infrastructure.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class KubernetesContainerInfo extends ContainerInfo {
  private String controllerType;
  private String controllerName;
  private String serviceName;
  private String podName;
  private String ip;

  @Builder
  public KubernetesContainerInfo(
      String clusterName, String controllerType, String controllerName, String serviceName, String podName, String ip) {
    super(clusterName);
    this.controllerType = controllerType;
    this.controllerName = controllerName;
    this.serviceName = serviceName;
    this.podName = podName;
    this.ip = ip;
  }
}
