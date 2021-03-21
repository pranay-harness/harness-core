package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCloudProviderAggregation implements Aggregation {
  private QLCloudProviderTypeAggregation typeAggregation;
}
