package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

import java.util.List;

@Value
@Builder
public class QLBillingDataLabelFilter implements Filter {
  private List<QLK8sLabelInput> labels;
  private QLIdOperator operator;

  @Override
  public QLIdOperator getOperator() {
    if (operator == null) {
      return QLIdOperator.IN;
    }
    return operator;
  }

  @Override
  public Object[] getValues() {
    return labels.toArray();
  }
}
