package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLArtifactSourceQueryParam {
  private String serviceId;
  private String applicationId;
  private String artifactSourceId;
}
