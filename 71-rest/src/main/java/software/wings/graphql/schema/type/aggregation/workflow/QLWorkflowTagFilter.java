package software.wings.graphql.schema.type.aggregation.workflow;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;

@Value
@Builder
public class QLWorkflowTagFilter implements EntityFilter {
  private QLWorkflowTagType entityType;
  private List<QLTagInput> tags;
}
