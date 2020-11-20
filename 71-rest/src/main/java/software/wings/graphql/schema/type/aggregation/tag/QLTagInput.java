package software.wings.graphql.schema.type.aggregation.tag;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;

@Value
@Builder
public class QLTagInput implements EntityFilter {
  private String name;
  private String value;
}
