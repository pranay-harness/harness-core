package software.wings.graphql.schema.type.aggregation.k8sLabel;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLK8sLabelFilter {
  private QLIdFilter accountId;
  private QLIdFilter cluster;
  private QLIdFilter namespace;
  private QLIdFilter workloadName;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;
}
