package software.wings.graphql.schema.type.aggregation.artifact;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLArtifactFilter implements EntityFilter {
  QLIdFilter artifact;
  QLIdFilter artifactSource;
  QLIdFilter artifactStreamType;
}
