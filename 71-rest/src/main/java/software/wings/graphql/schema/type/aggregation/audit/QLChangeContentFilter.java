package software.wings.graphql.schema.type.aggregation.audit;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLChangeContentFilter implements EntityFilter {
  String changeSetId;
  String resourceId;
}
