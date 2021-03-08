package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.LabelAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLBillingDataLabelAggregation implements LabelAggregation {
  String name;
}
