package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLPipelineConnection {
  private QLPageInfo pageInfo;
  private List<QLPipeline> nodes;
}
