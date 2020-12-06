package software.wings.graphql.schema.type.aggregation.environment;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEnvironmentTagFilter implements EntityFilter {
  private QLEnvironmentTagType entityType;
  private List<QLTagInput> tags;
}
