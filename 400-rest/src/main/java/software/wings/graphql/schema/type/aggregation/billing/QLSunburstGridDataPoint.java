package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(Module._380_CG_GRAPHQL)
public class QLSunburstGridDataPoint {
  String id;
  String name;
  String type;
  String clusterType;
  Number trend;
  Number value;
  int efficiencyScore;
}
