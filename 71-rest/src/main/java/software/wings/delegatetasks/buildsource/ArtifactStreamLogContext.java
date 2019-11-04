package software.wings.delegatetasks.buildsource;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class ArtifactStreamLogContext extends AutoLogContext {
  public ArtifactStreamLogContext(String artifactStreamId, String artifactStreamType, OverrideBehavior behavior) {
    super(ImmutableMap.of("artifactStreamId", artifactStreamId, "ArtifactStreamType", artifactStreamType), behavior);
  }
}
