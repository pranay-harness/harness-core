package software.wings.delegatetasks.buildsource;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._930_DELEGATE_TASKS)
public class ArtifactStreamNotFound extends RuntimeException {
  public ArtifactStreamNotFound(String artifactStreamId) {
    super(String.format("ArtifactServer %s could not be found", artifactStreamId));
  }
}
