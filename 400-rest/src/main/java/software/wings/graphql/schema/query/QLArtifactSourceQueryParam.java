package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLArtifactSourceQueryParam {
  private String serviceId;
  private String applicationId;
  private String artifactSourceId;
}
