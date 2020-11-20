package io.harness.delegate.task.citasks.cik8handler.params;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.delegate.beans.ci.pod.ContainerSecrets;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.SecretVarParams;
import io.harness.delegate.beans.ci.pod.SecretVolumeParams;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class GitCloneContainerParams extends ContainerParams {
  private final ConnectorDetails gitConnectorDetails;
  private final String branchName;
  private final String commitId;
  private final String stepExecVolumeName;
  private final String stepExecWorkingDir;

  @Builder
  public GitCloneContainerParams(ConnectorDetails gitConnectorDetails, String branchName, String commitId,
      String stepExecVolumeName, String stepExecWorkingDir, String name,
      ImageDetailsWithConnector imageDetailsWithConnector, List<String> commands, List<String> args, String workingDir,
      List<Integer> ports, Map<String, String> envVars, Map<String, SecretVarParams> secretEnvVars,
      Map<String, SecretVolumeParams> secretVolumes, Map<String, String> volumeToMountPath,
      ContainerResourceParams containerResourceParams, ContainerSecrets containerSecrets) {
    super(name, imageDetailsWithConnector, commands, args, workingDir, ports, envVars, secretEnvVars, secretVolumes,
        volumeToMountPath, containerResourceParams, containerSecrets);
    this.gitConnectorDetails = gitConnectorDetails;
    this.branchName = branchName;
    this.commitId = commitId;
    this.stepExecVolumeName = stepExecVolumeName;
    this.stepExecWorkingDir = stepExecWorkingDir;
  }

  @Override
  public ContainerParams.Type getType() {
    return ContainerParams.Type.K8_GIT_CLONE;
  }
}
