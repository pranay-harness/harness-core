package software.wings.delegatetasks.citasks.cik8handler.params;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.ci.pod.ContainerParams;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.ci.pod.SecretKeyParams;
import software.wings.beans.container.ImageDetails;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class GitCloneContainerParams extends ContainerParams {
  private GitFetchFilesConfig gitFetchFilesConfig;
  private String stepExecVolumeName;
  private String stepExecWorkingDir;

  @Builder
  public GitCloneContainerParams(GitFetchFilesConfig gitFetchFilesConfig, String stepExecVolumeName,
      String stepExecWorkingDir, String name, ImageDetails imageDetails, List<String> commands, List<String> args,
      String workingDir, List<Integer> ports, Map<String, String> envVars, Map<String, SecretKeyParams> secretEnvVars,
      Map<String, String> volumeToMountPath, ContainerResourceParams containerResourceParams) {
    super(name, imageDetails, commands, args, workingDir, ports, envVars, secretEnvVars, volumeToMountPath,
        containerResourceParams);
    this.gitFetchFilesConfig = gitFetchFilesConfig;
    this.stepExecVolumeName = stepExecVolumeName;
    this.stepExecWorkingDir = stepExecWorkingDir;
  }

  @Override
  public ContainerParams.Type getType() {
    return ContainerParams.Type.K8_GIT_CLONE;
  }
}