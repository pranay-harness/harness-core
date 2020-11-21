package software.wings.graphql.schema.type.aggregation.tag;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLEntityTypeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTagInUseFilter implements EntityFilter {
  private QLEntityTypeFilter entityType;
}
