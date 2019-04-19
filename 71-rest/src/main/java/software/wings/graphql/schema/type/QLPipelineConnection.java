package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLPipelineConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLPipeline> nodes;
}
