package software.wings.graphql.schema.type.aggregation.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

import java.util.List;

@Value
@Builder
public class QLChangeSetFilter implements EntityFilter {
  QLTimeRangeFilter time;
  List<QLEntityType> resources;
}
