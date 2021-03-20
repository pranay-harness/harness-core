package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLBillingDataTagFilter implements Filter {
  private QLBillingDataTagType entityType;
  private List<QLTagInput> tags;
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
    return tags.toArray();
  }
}
