package io.harness.delegate.task.citasks.cik8handler.pod;

/**
 * This class generates K8 pod spec for setting up CI build environment.
 */

import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.task.citasks.cik8handler.container.ContainerSpecBuilderResponse;
import io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.params.CIConstants;
import io.harness.delegate.task.citasks.cik8handler.params.GitCloneContainerParams;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodFluent;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CIK8PodSpecBuilder extends BasePodSpecBuilder {
  @Inject private GitCloneContainerSpecBuilder gitCloneContainerSpecBuilder;

  /**
   * Updates pod spec with git clone init container and CI specific pod attributes.
   */
  @Override
  protected void decorateSpec(
      PodParams<ContainerParams> podParams, PodFluent.SpecNested<PodBuilder> podBuilderSpecNested) {
    CIK8PodParams cik8PodParams;

    if (podParams.getType() == PodParams.Type.K8) {
      cik8PodParams = (CIK8PodParams) podParams;
    } else {
      log.error("Type mismatch: pod parameters is not of type: {}", PodParams.Type.K8);
      throw new InvalidRequestException("Type miss matched");
    }

    podBuilderSpecNested.withRestartPolicy(CIConstants.RESTART_POLICY);
    podBuilderSpecNested.withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS);
  }

  /**
   * Returns container spec to clone repository as part of init-container in K8.
   */
  private ContainerSpecBuilderResponse getInitContainer(
      ConnectorDetails gitConnector, String branchName, String commitId, String stepExecVolumeName, String workingDir) {
    GitCloneContainerParams gitCloneContainerParams = GitCloneContainerParams.builder()
                                                          .gitConnectorDetails(null)
                                                          .branchName(branchName)
                                                          .commitId(commitId)
                                                          .stepExecVolumeName(stepExecVolumeName)
                                                          .workingDir(workingDir)
                                                          .build();
    return gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
  }
}
