package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipeline {
  private String id;
  private String name;
  private String description;
}
