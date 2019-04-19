package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLWorkflow implements QLObject {
  private String id;
  private String name;
  private String description;
}
