package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEnvironment implements QLObject {
  private String id;
  private String name;
  private String description;
  private QLEnvironmentType type;
}
