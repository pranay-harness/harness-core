package software.wings.graphql.schema.type.aggregation.pipeline;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipelineTagFilter implements EntityFilter {
  private QLPipelineTagType entityType;
  private List<QLTagInput> tags;
}
