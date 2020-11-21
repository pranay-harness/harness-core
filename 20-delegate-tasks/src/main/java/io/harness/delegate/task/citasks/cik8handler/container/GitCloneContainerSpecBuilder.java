package io.harness.delegate.task.citasks.cik8handler.container;

/**
 * This class generates K8 container spec for a container to clone a git repository. It also implements cloning
 * private git repositories using basic auth and SSH keys.
 */

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.k8s.KubernetesConvention.getKubernetesGitSecretName;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.task.citasks.cik8handler.SecretSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants;
import io.harness.delegate.task.citasks.cik8handler.params.CIVolumeResponse;
import io.harness.delegate.task.citasks.cik8handler.params.GitCloneContainerParams;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.ImageDetails;

import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GitCloneContainerSpecBuilder extends BaseContainerSpecBuilder {
  private static final List<String> CONTAINER_BASH_EXEC_CMD = Arrays.asList("/bin/sh", "-c", "--");
  private static final Boolean GIT_CLONE_CONTAINER_PRIVILEGE = Boolean.FALSE;

  private static final String BASIC_AUTH_CONTAINER_ARG_FORMAT =
      "git clone -b %s --single-branch -- https://$(%s):$(%s)@%s %s/%s";
  private static final String SSH_CONTAINER_ARG_FORMAT = "GIT_SSH_COMMAND=\'ssh -i %s -o UserKnownHostsFile=/dev/null "
      + "-o StrictHostKeyChecking=no\' git clone -b %s --single-branch -- %s %s/%s";
  private static final String GIT_CHECKOUT_ARG_FORMAT = "; cd %s/%s; git checkout %s";

  /**
   * Returns container spec to clone a git repository using basic auth or SSH keys.
   */
  public ContainerSpecBuilderResponse createGitCloneSpec(GitCloneContainerParams gitCloneContainerParams) {
    ConnectorDetails gitConnectorDetails = gitCloneContainerParams.getGitConnectorDetails();
    if (gitConnectorDetails == null || gitConnectorDetails.getConnectorConfig() == null
        || gitConnectorDetails.getConnectorType() != ConnectorType.GIT) {
      log.info("Git repository information not provided. Skipping git clone container.");
      return null;
    }

    String branchName = gitCloneContainerParams.getBranchName();
    String commitId = gitCloneContainerParams.getCommitId();
    String workingDir = gitCloneContainerParams.getWorkingDir();
    String stepExecVolumeName = gitCloneContainerParams.getStepExecVolumeName();
    return createGitCloneSpecInternal(gitConnectorDetails, branchName, commitId, workingDir, stepExecVolumeName);
  }

  private ContainerSpecBuilderResponse createGitCloneSpecInternal(ConnectorDetails gitConnectorDetails,
      String branchName, String commitId, String workingDir, String stepExecVolumeName) {
    notNullCheck("gitConnectorDetails has to be specified for Remote", gitConnectorDetails);

    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnectorDetails.getConnectorConfig();
    if (isEmpty(branchName)) {
      branchName = gitConfigDTO.getBranchName();
    }

    notNullCheck("gitConfigDTO has to be specified", gitConfigDTO);
    notNullCheck("git repo url has to be specified", gitConfigDTO.getUrl());
    notNullCheck("branch has to be specified", branchName);
    notNullCheck("working directory has to be specified", workingDir);
    notNullCheck("step executor volume name has to be specified", stepExecVolumeName);
    List<String> containerArgs = getContainerArgs(gitConfigDTO, commitId, branchName, workingDir);

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(stepExecVolumeName, CIGitConstants.STEP_EXEC_VOLUME_MOUNT_PATH);
    GitCloneContainerParams updateContainerParam =
        GitCloneContainerParams.builder()
            .name(CIGitConstants.GIT_CLONE_CONTAINER_NAME)
            .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                           .imageDetails(ImageDetails.builder()
                                                             .name(CIGitConstants.GIT_CLONE_IMAGE_NAME)
                                                             .tag(CIGitConstants.GIT_CLONE_IMAGE_TAG)
                                                             .build())
                                           .build())
            .volumeToMountPath(volumeToMountPath)
            .commands(CONTAINER_BASH_EXEC_CMD)
            .args(containerArgs)
            .gitConnectorDetails(gitConnectorDetails)
            .stepExecWorkingDir(workingDir)
            .stepExecVolumeName(stepExecVolumeName)
            .build();

    return createSpec(updateContainerParam);
  }

  /**
   * Updates container spec with git clone container specific attributes. It adds security context and GIT SSH key
   * secrets volume to the container spec.
   */
  protected void decorateSpec(
      ContainerParams containerParams, ContainerSpecBuilderResponse containerSpecBuilderResponse) {
    GitCloneContainerParams gitCloneContainerParams;
    if (containerParams.getType() == ContainerParams.Type.K8_GIT_CLONE) {
      gitCloneContainerParams = (GitCloneContainerParams) containerParams;
    } else {
      log.error("Type mismatch: container parameters is not of type: {}", ContainerParams.Type.K8_GIT_CLONE);
      throw new InvalidRequestException("Type miss matched");
    }

    ContainerBuilder containerBuilder = containerSpecBuilderResponse.getContainerBuilder();
    ConnectorDetails gitConnectorDetails = gitCloneContainerParams.getGitConnectorDetails();
    GitConfigDTO gitConfig = (GitConfigDTO) gitConnectorDetails.getConnectorConfig();
    String gitSecret = getKubernetesGitSecretName(gitConfig.getUrl());

    ArrayList<EnvVar> envVars = getEnvVars(gitConfig, gitSecret);
    containerBuilder.withEnv(envVars);

    List<Volume> volumes = new ArrayList<>();
    if (containerSpecBuilderResponse.getVolumes() != null) {
      volumes = containerSpecBuilderResponse.getVolumes();
    }

    // Mounting GIT SSH key from secret store
    CIVolumeResponse ciVolumeResponse = getGitSecretVolume(gitConfig, gitSecret);
    if (ciVolumeResponse != null) {
      containerBuilder.addToVolumeMounts(ciVolumeResponse.getVolumeMount());
      volumes.add(ciVolumeResponse.getVolume());
    }

    SecurityContext securityContext =
        new SecurityContextBuilder().withPrivileged(GIT_CLONE_CONTAINER_PRIVILEGE).build();
    containerBuilder.withSecurityContext(securityContext);

    containerSpecBuilderResponse.setContainerBuilder(containerBuilder);
    containerSpecBuilderResponse.setVolumes(volumes);
  }

  /**
   * Returns environment variables for git clone container spec.
   */
  private ArrayList<EnvVar> getEnvVars(GitConfigDTO gitConfig, String gitSecret) {
    ArrayList<EnvVar> envVars = new ArrayList<>();

    if (gitConfig.getGitAuthType() == GitAuthType.HTTP) {
      EnvVarSource userNameSource = new EnvVarSourceBuilder()
                                        .withSecretKeyRef(new SecretKeySelectorBuilder()
                                                              .withName(gitSecret)
                                                              .withKey(SecretSpecBuilder.GIT_SECRET_USERNAME_KEY)
                                                              .build())
                                        .build();
      envVars.add(
          new EnvVarBuilder().withName(CIGitConstants.GIT_USERNAME_ENV_VAR).withValueFrom(userNameSource).build());

      EnvVarSource pwdSource = new EnvVarSourceBuilder()
                                   .withSecretKeyRef(new SecretKeySelectorBuilder()
                                                         .withName(gitSecret)
                                                         .withKey(SecretSpecBuilder.GIT_SECRET_PWD_KEY)
                                                         .build())
                                   .build();
      envVars.add(new EnvVarBuilder().withName(CIGitConstants.GIT_PASS_ENV_VAR).withValueFrom(pwdSource).build());
    }
    // Add log service endpoint
    envVars.add(new EnvVarBuilder()
                    .withName(CIGitConstants.LOG_SERVICE_ENDPOINT_VARIABLE)
                    .withValue(CIGitConstants.LOG_SERVICE_ENDPOINT_VARIABLE_VALUE)
                    .build());
    log.info(envVars.toString());
    return envVars;
  }

  /**
   * Returns container arguments to clone a git repository using either basic auth or SSH keys.
   */
  private List<String> getContainerArgs(
      GitConfigDTO gitConfigDTO, String commitId, String branchName, String workingDir) {
    String repoUrl = gitConfigDTO.getUrl();
    String volMountPath = CIGitConstants.STEP_EXEC_VOLUME_MOUNT_PATH;
    List<String> containerArgs = new ArrayList<>();
    String containerArg;
    if (gitConfigDTO.getGitAuthType() == GitAuthType.HTTP) {
      String gitUrl = repoUrl.substring(repoUrl.indexOf("://") + 3);
      containerArg = String.format(BASIC_AUTH_CONTAINER_ARG_FORMAT, branchName, CIGitConstants.GIT_USERNAME_ENV_VAR,
          CIGitConstants.GIT_PASS_ENV_VAR, gitUrl, volMountPath, workingDir);
    } else if (gitConfigDTO.getGitAuthType() == GitAuthType.SSH) {
      String sshKeyPath =
          String.format("%s/%s", CIGitConstants.GIT_SSH_VOL_MOUNT_PATH, SecretSpecBuilder.GIT_SECRET_SSH_KEY);
      containerArg = String.format(SSH_CONTAINER_ARG_FORMAT, sshKeyPath, branchName, repoUrl, volMountPath, workingDir);
    } else {
      String errMsg =
          String.format("Invalid GIT authentication scheme %s for repo %s", gitConfigDTO.getGitAuthType(), repoUrl);
      log.error(errMsg);
      throw new InvalidArgumentsException(errMsg, WingsException.USER);
    }

    if (commitId != null) {
      containerArg += String.format(GIT_CHECKOUT_ARG_FORMAT, volMountPath, workingDir, commitId);
    }
    containerArgs.add(containerArg);
    return containerArgs;
  }

  /**
   * Creates a volume with GIT SSH keys secret and returns it. Git clone container will mount this volume to access the
   * SSH key required to clone a git repository.
   */
  private CIVolumeResponse getGitSecretVolume(GitConfigDTO gitConfig, String gitSecret) {
    if (gitConfig.getGitAuthType() == GitAuthType.SSH) {
      VolumeMount volumeMount = new VolumeMountBuilder()
                                    .withName(CIGitConstants.GIT_SSH_VOL_NAME)
                                    .withMountPath(CIGitConstants.GIT_SSH_VOL_MOUNT_PATH)
                                    .build();
      Volume volume = new VolumeBuilder()
                          .withName(CIGitConstants.GIT_SSH_VOL_NAME)
                          .withSecret(new SecretVolumeSourceBuilder()
                                          .withDefaultMode(CIGitConstants.GIT_SSH_VOL_DEFAULT_MODE)
                                          .withSecretName(gitSecret)
                                          .build())
                          .build();
      return CIVolumeResponse.builder().volume(volume).volumeMount(volumeMount).build();
    }
    return null;
  }
}
