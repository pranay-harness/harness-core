package software.wings.graphql.schema.type.instance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLK8sContainer {
  private String containerId;
  private String name;
  private String image;
}
