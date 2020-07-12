package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;

@Value
@Builder
public class QLCETagFilter implements Filter {
  private QLCETagType entityType;
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
