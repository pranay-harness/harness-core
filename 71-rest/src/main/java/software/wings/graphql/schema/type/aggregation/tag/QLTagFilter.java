package software.wings.graphql.schema.type.aggregation.tag;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

@Value
@Builder
public class QLTagFilter implements EntityFilter {
  private QLEntityType entityType;
  private String name;
  private String[] values;
}