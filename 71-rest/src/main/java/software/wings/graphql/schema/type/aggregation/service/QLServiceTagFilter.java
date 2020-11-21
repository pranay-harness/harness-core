package software.wings.graphql.schema.type.aggregation.service;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLServiceTagFilter implements EntityFilter {
  private QLServiceTagType entityType;
  private List<QLTagInput> tags;
}
