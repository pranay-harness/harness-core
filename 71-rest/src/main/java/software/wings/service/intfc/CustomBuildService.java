package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface CustomBuildService extends BuildService {
  @DelegateTaskType(TaskType.CUSTOM_GET_BUILDS)
  List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes);
}
