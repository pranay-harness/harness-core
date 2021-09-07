package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CV)
public class QLExecutionStatusFilter implements Filter {
  private QLEnumOperator operator;
  private QLExecutionStatusType[] values;
}
