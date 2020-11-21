package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLStatsBreakdownInfo implements QLObject {
  Number trend;
  Number total;
  Number utilized;
  Number idle;
  Number unallocated;
}
