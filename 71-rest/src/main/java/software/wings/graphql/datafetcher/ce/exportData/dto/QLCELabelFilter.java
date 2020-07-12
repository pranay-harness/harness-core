package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLK8sLabelInput;

import java.util.List;

@Value
@Builder
public class QLCELabelFilter implements Filter {
  private List<QLK8sLabelInput> labels;

  @Override
  public QLIdOperator getOperator() {
    return QLIdOperator.IN;
  }

  @Override
  public Object[] getValues() {
    return labels.toArray();
  }
}
