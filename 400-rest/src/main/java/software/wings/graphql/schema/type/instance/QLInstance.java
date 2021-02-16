package software.wings.graphql.schema.type.instance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.artifact.QLArtifact;

@TargetModule(Module._380_CG_GRAPHQL)
public interface QLInstance {
  String getId();
  QLInstanceType getType();
  String getEnvironmentId();
  String getApplicationId();
  String getServiceId();
  QLArtifact getArtifact();
}
