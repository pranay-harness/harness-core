package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
