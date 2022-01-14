package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLDeploymentSortCriteria {
  private QLDeploymentSortType sortType;
  private QLSortOrder sortOrder;
}
