package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLSunburstChartData implements QLData {
  Double totalCost;
  List<QLSunburstGridDataPoint> gridData;
}
