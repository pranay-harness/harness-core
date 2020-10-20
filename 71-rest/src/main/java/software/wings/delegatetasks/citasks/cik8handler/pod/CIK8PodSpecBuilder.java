package software.wings.delegatetasks.citasks.cik8handler.pod;

/**
 * This class generates K8 pod spec for setting up CI build environment.
 */

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodFluent;
import io.fabric8.kubernetes.api.model.Volume;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerParams;
import software.wings.beans.ci.pod.PodParams;
import software.wings.delegatetasks.citasks.cik8handler.container.ContainerSpecBuilderResponse;
import software.wings.delegatetasks.citasks.cik8handler.container.GitCloneContainerSpecBuilder;
import software.wings.delegatetasks.citasks.cik8handler.params.CIConstants;
import software.wings.delegatetasks.citasks.cik8handler.params.GitCloneContainerParams;

import java.util.ArrayList;
import java.util.List;

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
    CIK8PodParams cik8PodParams = null;

    if (podParams.getType() == PodParams.Type.K8) {
      cik8PodParams = (CIK8PodParams) podParams;
    } else {
      logger.error("Type mismatch: pod parameters is not of type: {}", PodParams.Type.K8);
      throw new InvalidRequestException("Type miss matched");
    }

    ContainerSpecBuilderResponse initCtrSpecBuilderResponse = getInitContainer(cik8PodParams.getGitFetchFilesConfig(),
        cik8PodParams.getStepExecVolumeName(), cik8PodParams.getStepExecWorkingDir());

    List<Container> initContainers = new ArrayList<>();
    List<Volume> volumes = new ArrayList<>();
    ContainerBuilder containerBuilder =
        initCtrSpecBuilderResponse == null ? null : initCtrSpecBuilderResponse.getContainerBuilder();
    if (containerBuilder != null) {
      initContainers.add(containerBuilder.build());
      final List<Volume> initCtrVolumeList = initCtrSpecBuilderResponse.getVolumes();
      if (initCtrVolumeList != null) {
        volumes = initCtrVolumeList;
      }
    }

    podBuilderSpecNested.addAllToInitContainers(initContainers);
    podBuilderSpecNested.addAllToVolumes(volumes);
    podBuilderSpecNested.withRestartPolicy(CIConstants.RESTART_POLICY);
    podBuilderSpecNested.withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS);
  }

  /**
   * Returns container spec to clone repository as part of init-container in K8.
   */
  private ContainerSpecBuilderResponse getInitContainer(
      GitFetchFilesConfig gitFetchFilesConfig, String stepExecVolumeName, String workingDir) {
    GitCloneContainerParams gitCloneContainerParams = GitCloneContainerParams.builder()
                                                          .gitFetchFilesConfig(gitFetchFilesConfig)
                                                          .stepExecVolumeName(stepExecVolumeName)
                                                          .workingDir(workingDir)
                                                          .build();
    return gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
  }
}