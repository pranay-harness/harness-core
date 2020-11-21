package software.wings.graphql.schema.type.aggregation.trigger;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTriggerTagFilter implements EntityFilter {
  private QLTriggerTagType entityType;
  private List<QLTagInput> tags;
}
