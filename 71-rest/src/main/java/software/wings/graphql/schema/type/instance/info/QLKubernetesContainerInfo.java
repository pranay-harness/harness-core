package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 09/05/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QLKubernetesContainerInfo extends QLContainerInfo {
  private String controllerType;
  private String controllerName;
  private String serviceName;
  private String podName;
  private String ip;
  private String namespace;

  @Builder
  public QLKubernetesContainerInfo(String clusterName, String controllerType, String controllerName, String serviceName,
      String podName, String ip, String namespace) {
    super(clusterName);
    this.controllerType = controllerType;
    this.controllerName = controllerName;
    this.serviceName = serviceName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
  }
}
