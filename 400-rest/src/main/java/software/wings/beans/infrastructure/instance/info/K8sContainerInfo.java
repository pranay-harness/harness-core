package software.wings.beans.infrastructure.instance.info;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sContainerInfo {
  private String containerId;
  private String name;
  private String image;
}
