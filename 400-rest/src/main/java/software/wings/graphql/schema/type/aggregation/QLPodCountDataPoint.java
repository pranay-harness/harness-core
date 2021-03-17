package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLPodCountDataPoint {
  QLReference key;
  Number value;
}
