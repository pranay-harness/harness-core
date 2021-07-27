package software.wings.graphql.schema.type.aggregation.instance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public class QLInstanceAggregation implements Aggregation {
  private QLInstanceEntityAggregation entityAggregation;
  private QLTimeSeriesAggregation timeAggregation;
  private QLInstanceTagAggregation tagAggregation;
}
