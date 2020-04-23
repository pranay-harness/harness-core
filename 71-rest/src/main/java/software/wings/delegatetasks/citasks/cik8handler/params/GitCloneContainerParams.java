package software.wings.delegatetasks.citasks.cik8handler.params;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.ci.pod.ContainerParams;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.container.ImageDetails;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class GitCloneContainerParams extends ContainerParams {
  private GitFetchFilesConfig gitFetchFilesConfig;
  private String workingDir;
  private String stepExecVolumeName;

  @Builder
  public GitCloneContainerParams(GitFetchFilesConfig gitFetchFilesConfig, String workingDir, String stepExecVolumeName,
      String name, ImageDetails imageDetails, List<String> commands, List<String> args, Map<String, String> envVars,
      Map<String, String> volumeToMountPath, ContainerResourceParams containerResourceParams) {
    super(name, imageDetails, commands, args, envVars, volumeToMountPath, containerResourceParams);
    this.gitFetchFilesConfig = gitFetchFilesConfig;
    this.workingDir = workingDir;
    this.stepExecVolumeName = stepExecVolumeName;
  }

  @Override
  public ContainerParams.Type getType() {
    return ContainerParams.Type.K8_GIT_CLONE;
  }
}