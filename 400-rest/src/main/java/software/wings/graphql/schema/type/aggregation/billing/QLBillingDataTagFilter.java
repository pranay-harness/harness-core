package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
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
