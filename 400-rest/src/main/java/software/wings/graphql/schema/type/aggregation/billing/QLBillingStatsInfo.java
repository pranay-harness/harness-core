package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBillingStatsInfo implements QLObject {
  String statsLabel;
  String statsDescription;
  String statsValue;
  Number statsTrend;
}
