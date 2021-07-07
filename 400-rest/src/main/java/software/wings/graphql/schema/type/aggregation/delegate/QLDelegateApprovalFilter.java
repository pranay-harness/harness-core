package software.wings.graphql.schema.type.aggregation.delegate;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

@Value
@Builder
public class QLDelegateApprovalFilter implements Filter {
  private QLEnumOperator operator;
  private QLDelegateApprovalFilter[] values;
}
