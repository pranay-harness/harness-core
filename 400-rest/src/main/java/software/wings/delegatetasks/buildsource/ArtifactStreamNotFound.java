package software.wings.delegatetasks.buildsource;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ArtifactStreamNotFound extends RuntimeException {
  public ArtifactStreamNotFound(String artifactStreamId) {
    super(String.format("ArtifactServer %s could not be found", artifactStreamId));
  }
}
