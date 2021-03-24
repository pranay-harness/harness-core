package software.wings.graphql.schema.type.aggregation.artifact;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLArtifactFilter implements EntityFilter {
  QLIdFilter artifact;
  QLIdFilter artifactSource;
  QLIdFilter artifactStreamType;
}
