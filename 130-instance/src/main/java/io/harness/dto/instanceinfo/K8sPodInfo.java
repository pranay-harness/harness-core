package io.harness.dto.instanceinfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class K8sPodInfo extends ContainerInfo {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<K8sContainerInfo> containers;
  private String blueGreenColor;
}
