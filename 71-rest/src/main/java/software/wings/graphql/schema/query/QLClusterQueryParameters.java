package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLClusterQueryParameters {
  private String clusterId;
}
