package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import software.wings.utils.ArtifactType;

@Value
@Builder
public class QLService {
  private String id;
  private String name;
  private String description;

  ArtifactType artifactType;
}
