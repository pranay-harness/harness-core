package software.wings.graphql.schema.type.aggregation.k8sLabel;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

@Value
@Builder
public class QLK8sLabelFilter {
  private QLIdFilter cluster;
  private QLIdFilter namespace;
  private QLIdFilter workloadName;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;
}
