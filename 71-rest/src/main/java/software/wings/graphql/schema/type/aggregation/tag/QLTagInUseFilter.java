package software.wings.graphql.schema.type.aggregation.tag;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLEntityTypeFilter;

@Value
@Builder
public class QLTagInUseFilter implements EntityFilter {
  private QLEntityTypeFilter entityType;
}
