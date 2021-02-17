package software.wings.graphql.schema.type.aggregation.audit;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLChangeContentFilter implements EntityFilter {
  String changeSetId;
  String resourceId;
}
