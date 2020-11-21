package software.wings.graphql.schema.type.aggregation.audit;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLChangeSetFilter implements EntityFilter {
  QLTimeRangeFilter time;
  List<QLEntityType> resources;
}
