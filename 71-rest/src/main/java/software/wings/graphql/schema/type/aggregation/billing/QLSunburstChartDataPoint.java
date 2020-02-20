package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLSunburstChartDataPoint {
  String id;
  String parent;
  String name;
  String type;
  Number value;
  String clusterType;
  String instanceType;
  QLSunburstGridDataPoint metadata;
}
