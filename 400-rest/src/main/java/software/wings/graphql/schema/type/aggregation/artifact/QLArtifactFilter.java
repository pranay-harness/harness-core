package software.wings.graphql.schema.type.aggregation.artifact;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLArtifactFilter implements EntityFilter {
  QLIdFilter artifact;
  QLIdFilter artifactSource;
  QLIdFilter artifactStreamType;
}
