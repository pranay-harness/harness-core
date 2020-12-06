package software.wings.graphql.schema.type.aggregation.tag;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTagInput implements EntityFilter {
  private String name;
  private String value;
}
