package software.wings.graphql.schema.type.aggregation.budget;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBudgetTableListData {
  List<QLBudgetTableData> data;
}
