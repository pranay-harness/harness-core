package software.wings.graphql.schema.type.aggregation.pipeline;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLPipelineAggregation implements Aggregation {
  private QLPipelineEntityAggregation entityAggregation;
  private QLPipelineTagAggregation tagAggregation;
}
