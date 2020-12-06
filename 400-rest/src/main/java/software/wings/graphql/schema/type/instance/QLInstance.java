package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

public interface QLInstance {
  String getId();
  QLInstanceType getType();
  String getEnvironmentId();
  String getApplicationId();
  String getServiceId();
  QLArtifact getArtifact();
}
