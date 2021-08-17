package software.wings.graphql.schema.type.aggregation.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLVerificationAggregation implements Aggregation {
  private QLCVEntityAggregation entityAggregation;
  private QLTimeSeriesAggregation timeAggregation;
  private QLDeploymentTagAggregation tagAggregation;
}
