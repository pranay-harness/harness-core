package software.wings.graphql.schema.type.aggregation.instance;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLInstanceTagFilter implements EntityFilter {
  private QLInstanceTagType entityType;
  private List<QLTagInput> tags;
}
