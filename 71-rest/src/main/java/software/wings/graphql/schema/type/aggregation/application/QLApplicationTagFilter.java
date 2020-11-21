package software.wings.graphql.schema.type.aggregation.application;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLApplicationTagFilter implements EntityFilter {
  private QLApplicationTagType entityType;
  private List<QLTagInput> tags;
}
