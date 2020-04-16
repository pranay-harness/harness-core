package software.wings.delegatetasks.buildsource;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.artifact.ArtifactStream;

public class ArtifactStreamLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(ArtifactStream.class);
  public static final String ARTIFACT_STREAM_TYPE = "artifactStreamType";

  public ArtifactStreamLogContext(String artifactStreamId, String artifactStreamType, OverrideBehavior behavior) {
    super(ImmutableMap.of(ID, artifactStreamId, ARTIFACT_STREAM_TYPE, artifactStreamType), behavior);
  }

  public ArtifactStreamLogContext(String artifactStreamId, OverrideBehavior behavior) {
    super(ImmutableMap.of(ID, artifactStreamId), behavior);
  }
}
