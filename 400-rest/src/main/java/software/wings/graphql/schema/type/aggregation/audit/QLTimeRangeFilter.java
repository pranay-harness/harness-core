package software.wings.graphql.schema.type.aggregation.audit;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLTimeRange;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLTimeRangeFilter {
  private QLTimeRange specific;
  private QLRelativeTimeRange relative;
}
