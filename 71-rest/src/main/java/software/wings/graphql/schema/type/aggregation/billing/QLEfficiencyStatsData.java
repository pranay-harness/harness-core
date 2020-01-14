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
public class QLEfficiencyStatsData implements QLData {
  QLContextInfo context;
  QLStatsBreakdownInfo efficiencyBreakdown;
  List<QLResourceStatsInfo> resourceBreakdown;
}
