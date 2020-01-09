package software.wings.graphql.schema.type.aggregation.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;

@Value
@Builder
public class QLChangeContentFilter implements EntityFilter {
  String changeSetId;
  String resourceId;
}
