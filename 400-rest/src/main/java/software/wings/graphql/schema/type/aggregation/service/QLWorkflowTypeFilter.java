package software.wings.graphql.schema.type.aggregation.service;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLWorkflowTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLWorkflowType[] values;
}