package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLWorkflowTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLWorkflowType[] values;
}