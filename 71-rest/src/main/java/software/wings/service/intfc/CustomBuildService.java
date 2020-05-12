package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface CustomBuildService extends BuildService {
  @Override
  @DelegateTaskType(TaskType.CUSTOM_GET_BUILDS)
  List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes);

  @Override
  @DelegateTaskType(TaskType.CUSTOM_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(ArtifactStreamAttributes artifactStreamAttributes);
}
