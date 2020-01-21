package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLSunburstChartData implements QLData {
  List<QLSunburstChartDataPoint> data;
  List<QLSunburstGridDataPoint> gridData;
}
