package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.customrepository.CustomRepositoryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.CustomBuildService;

import java.util.List;

@OwnedBy(CDC)
@Singleton
public class CustomBuildServiceImpl implements CustomBuildService {
  @Inject private CustomRepositoryService customRepositoryService;

  @Override
  @DelegateTaskType(TaskType.CUSTOM_GET_BUILDS)
  public List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes) {
    // NOTE: Custom artifact stream doesn't support labels so the last two arguments can be null.
    return wrapNewBuildsWithLabels(
        customRepositoryService.getBuilds(artifactStreamAttributes), artifactStreamAttributes, null, null);
  }

  @Override
  @DelegateTaskType(TaskType.CUSTOM_VALIDATE_ARTIFACT_STREAM)
  public boolean validateArtifactSource(ArtifactStreamAttributes artifactStreamAttributes) {
    return customRepositoryService.validateArtifactSource(artifactStreamAttributes);
  }
}
