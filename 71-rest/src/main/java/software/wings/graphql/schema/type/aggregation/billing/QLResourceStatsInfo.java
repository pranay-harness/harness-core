package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLResourceStatsInfo implements QLObject {
  String type;
  QLStatsBreakdownInfo info;
}
