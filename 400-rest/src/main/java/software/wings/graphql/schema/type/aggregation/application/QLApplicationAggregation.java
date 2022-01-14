package software.wings.graphql.schema.type.aggregation.application;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLApplicationAggregation implements Aggregation {
  private QLApplicationEntityAggregation entityAggregation;
  private QLApplicationTagAggregation tagAggregation;
}
