package software.wings.graphql.schema.type.aggregation.budget;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBudgetDataList {
  List<QLBudgetData> data;
}
