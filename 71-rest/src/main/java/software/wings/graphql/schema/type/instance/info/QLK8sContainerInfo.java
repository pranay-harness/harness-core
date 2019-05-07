package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLK8sContainerInfo {
  private String containerId;
  private String name;
  private String image;
}
