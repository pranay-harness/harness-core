package software.wings.graphql.schema.type.aggregation.budget;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.BUDGET)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBudgetNotifications {
  int count;
}
