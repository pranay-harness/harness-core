package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLFilterValuesListData implements QLData {
  List<QLFilterValuesData> data;
  boolean isHourlyDataPresent;
}
