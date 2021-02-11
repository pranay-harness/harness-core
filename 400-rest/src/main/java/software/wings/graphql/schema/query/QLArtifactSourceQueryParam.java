package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLArtifactSourceQueryParam {
  private String serviceId;
  private String applicationId;
  private String artifactSourceId;
}
