package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeploymentTagFilter implements Filter {
  private QLDeploymentTagType entityType;
  private List<QLTagInput> tags;

  @Override
  public QLIdOperator getOperator() {
    return QLIdOperator.IN;
  }

  @Override
  public Object[] getValues() {
    return tags.toArray();
  }
}
