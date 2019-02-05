package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.customrepository.CustomRepositoryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.CustomBuildService;

import java.util.List;

@Singleton
public class CustomBuildServiceImpl implements CustomBuildService {
  @Inject private CustomRepositoryService customRepositoryService;

  @DelegateTaskType(TaskType.CUSTOM_GET_BUILDS)
  public List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes) {
    return customRepositoryService.getBuilds(artifactStreamAttributes);
  }

  @DelegateTaskType(TaskType.CUSTOM_VALIDATE_ARTIFACT_STREAM)
  public boolean validateArtifactSource(ArtifactStreamAttributes artifactStreamAttributes) {
    return customRepositoryService.validateArtifactSource(artifactStreamAttributes);
  }
}
