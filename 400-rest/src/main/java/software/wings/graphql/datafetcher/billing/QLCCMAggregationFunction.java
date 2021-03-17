package software.wings.graphql.datafetcher.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLCCMAggregationFunction {
  QLCCMAggregateOperation operationType;
  String columnName;
}
