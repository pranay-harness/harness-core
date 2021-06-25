package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CV)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLExecutionStatusFilter implements Filter {
  private QLEnumOperator operator;
  private QLExecutionStatusType[] values;
}
