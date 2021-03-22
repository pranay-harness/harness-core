package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLOrchestrationWorkflowTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLOrchestrationWorkflowType[] values;
}