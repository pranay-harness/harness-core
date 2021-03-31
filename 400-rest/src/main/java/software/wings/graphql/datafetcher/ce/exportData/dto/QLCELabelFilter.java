package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLK8sLabelInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
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
