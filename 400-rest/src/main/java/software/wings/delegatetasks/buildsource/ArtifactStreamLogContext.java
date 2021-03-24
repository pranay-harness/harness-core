package software.wings.delegatetasks.buildsource;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.artifact.ArtifactStream;

import com.google.common.collect.ImmutableMap;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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
