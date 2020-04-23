package software.wings.delegatetasks.citasks.cik8handler.container;

/**
 * This class generates K8 container spec for a container to clone a git repository. It also implements cloning
 * private git repositories using basic auth and SSH keys.
 */

import static io.harness.validation.Validator.notNullCheck;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_CLONE_CONTAINER_NAME;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_CLONE_IMAGE_NAME;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_CLONE_IMAGE_TAG;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_PASS_ENV_VAR;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_SSH_VOL_DEFAULT_MODE;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_SSH_VOL_MOUNT_PATH;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_SSH_VOL_NAME;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants.GIT_USERNAME_ENV_VAR;
import static software.wings.utils.KubernetesConvention.getKubernetesGitSecretName;

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
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.ci.pod.ContainerParams;
import software.wings.beans.container.ImageDetails;
import software.wings.delegatetasks.citasks.cik8handler.SecretSpecBuilder;
import software.wings.delegatetasks.citasks.cik8handler.params.CIGitConstants;
import software.wings.delegatetasks.citasks.cik8handler.params.CIVolumeResponse;
import software.wings.delegatetasks.citasks.cik8handler.params.GitCloneContainerParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class GitCloneContainerSpecBuilder extends BaseContainerSpecBuilder {
  private static final List<String> CONTAINER_BASH_EXEC_CMD = Arrays.asList("/bin/sh", "-c", "--");
  private static final Boolean GIT_CLONE_CONTAINER_READ_ONLY_ROOT_FS = Boolean.TRUE;
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
    GitFetchFilesConfig gitFetchFilesConfig = gitCloneContainerParams.getGitFetchFilesConfig();
    if (gitFetchFilesConfig == null || gitFetchFilesConfig.getGitConfig() == null
        || gitFetchFilesConfig.getGitConfig().getRepoUrl() == null) {
      logger.info("Git repository information not provided. Skipping git clone container.");
      return null;
    }

    String workingDir = gitCloneContainerParams.getWorkingDir();
    String stepExecVolumeName = gitCloneContainerParams.getStepExecVolumeName();
    return createGitCloneSpecInternal(gitFetchFilesConfig, workingDir, stepExecVolumeName);
  }

  private ContainerSpecBuilderResponse createGitCloneSpecInternal(
      GitFetchFilesConfig gitFetchFilesConfig, String workingDir, String stepExecVolumeName) {
    notNullCheck("gitFetchFileConfig has to be specified for Remote", gitFetchFilesConfig);

    GitConfig gitConfig = gitFetchFilesConfig.getGitConfig();
    GitFileConfig gitFileConfig = gitFetchFilesConfig.getGitFileConfig();
    notNullCheck("gitConfig has to be specified", gitConfig);
    notNullCheck("gitFileConfig has to be specified", gitFileConfig);
    notNullCheck("branch has to be specified", gitFileConfig.getBranch());
    notNullCheck("Working director has to be specified", workingDir);
    notNullCheck("Step executor volume name has to be specified", stepExecVolumeName);
    List<String> containerArgs = getContainerArgs(gitFileConfig, gitConfig, workingDir);

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(stepExecVolumeName, CIGitConstants.STEP_EXEC_VOLUME_MOUNT_PATH);
    GitCloneContainerParams updateContainerParam =
        GitCloneContainerParams.builder()
            .name(GIT_CLONE_CONTAINER_NAME)
            .imageDetails(ImageDetails.builder().name(GIT_CLONE_IMAGE_NAME).tag(GIT_CLONE_IMAGE_TAG).build())
            .volumeToMountPath(volumeToMountPath)
            .commands(CONTAINER_BASH_EXEC_CMD)
            .args(containerArgs)
            .gitFetchFilesConfig(gitFetchFilesConfig)
            .workingDir(workingDir)
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
    GitCloneContainerParams gitCloneContainerParams = null;
    if (containerParams.getType() == ContainerParams.Type.K8_GIT_CLONE) {
      gitCloneContainerParams = (GitCloneContainerParams) containerParams;
    } else {
      logger.error("Type mismatch: container parameters is not of type: {}", ContainerParams.Type.K8_GIT_CLONE);
      throw new InvalidRequestException("Type miss matched");
    }

    ContainerBuilder containerBuilder = containerSpecBuilderResponse.getContainerBuilder();
    GitFetchFilesConfig gitFetchFilesConfig = gitCloneContainerParams.getGitFetchFilesConfig();
    GitConfig gitConfig = gitFetchFilesConfig.getGitConfig();
    String gitSecret = getKubernetesGitSecretName(gitConfig.getRepoUrl());

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

    SecurityContext securityContext = new SecurityContextBuilder()
                                          .withReadOnlyRootFilesystem(GIT_CLONE_CONTAINER_READ_ONLY_ROOT_FS)
                                          .withPrivileged(GIT_CLONE_CONTAINER_PRIVILEGE)
                                          .build();
    containerBuilder.withSecurityContext(securityContext);

    containerSpecBuilderResponse.setContainerBuilder(containerBuilder);
    containerSpecBuilderResponse.setVolumes(volumes);
  }

  /**
   * Returns environment variables for git clone container spec.
   */
  private ArrayList<EnvVar> getEnvVars(GitConfig gitConfig, String gitSecret) {
    ArrayList<EnvVar> envVars = new ArrayList<>();

    if (gitConfig.getAuthenticationScheme() == HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD) {
      EnvVarSource userNameSource = new EnvVarSourceBuilder()
                                        .withSecretKeyRef(new SecretKeySelectorBuilder()
                                                              .withName(gitSecret)
                                                              .withKey(SecretSpecBuilder.GIT_SECRET_USERNAME_KEY)
                                                              .build())
                                        .build();
      envVars.add(new EnvVarBuilder().withName(GIT_USERNAME_ENV_VAR).withValueFrom(userNameSource).build());

      EnvVarSource pwdSource = new EnvVarSourceBuilder()
                                   .withSecretKeyRef(new SecretKeySelectorBuilder()
                                                         .withName(gitSecret)
                                                         .withKey(SecretSpecBuilder.GIT_SECRET_PWD_KEY)
                                                         .build())
                                   .build();
      envVars.add(new EnvVarBuilder().withName(GIT_PASS_ENV_VAR).withValueFrom(pwdSource).build());
    }
    return envVars;
  }

  /**
   * Returns container arguments to clone a git repository using either basic auth or SSH keys.
   */
  private List<String> getContainerArgs(GitFileConfig gitFileConfig, GitConfig gitConfig, String workingDir) {
    String branchName = gitFileConfig.getBranch();
    String commitId = gitFileConfig.getCommitId();
    String volMountPath = CIGitConstants.STEP_EXEC_VOLUME_MOUNT_PATH;
    List<String> containerArgs = new ArrayList<>();
    String containerArg;
    if (gitConfig.getAuthenticationScheme() == HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD) {
      String gitUrl = gitConfig.getRepoUrl().substring(gitConfig.getRepoUrl().indexOf("://") + 3);
      containerArg = String.format(BASIC_AUTH_CONTAINER_ARG_FORMAT, branchName, GIT_USERNAME_ENV_VAR, GIT_PASS_ENV_VAR,
          gitUrl, volMountPath, workingDir);
    } else if (gitConfig.getAuthenticationScheme() == HostConnectionAttributes.AuthenticationScheme.SSH_KEY) {
      String sshKeyPath = String.format("%s/%s", GIT_SSH_VOL_MOUNT_PATH, SecretSpecBuilder.GIT_SECRET_SSH_KEY);
      containerArg = String.format(
          SSH_CONTAINER_ARG_FORMAT, sshKeyPath, branchName, gitConfig.getRepoUrl(), volMountPath, workingDir);
    } else {
      String errMsg =
          String.format("Invalid GIT authentication scheme %s", gitConfig.getAuthenticationScheme().toString());
      logger.error(errMsg);
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
  private CIVolumeResponse getGitSecretVolume(GitConfig gitConfig, String gitSecret) {
    if (gitConfig.getAuthenticationScheme() == HostConnectionAttributes.AuthenticationScheme.SSH_KEY) {
      VolumeMount volumeMount =
          new VolumeMountBuilder().withName(GIT_SSH_VOL_NAME).withMountPath(GIT_SSH_VOL_MOUNT_PATH).build();
      Volume volume = new VolumeBuilder()
                          .withName(GIT_SSH_VOL_NAME)
                          .withSecret(new SecretVolumeSourceBuilder()
                                          .withDefaultMode(GIT_SSH_VOL_DEFAULT_MODE)
                                          .withSecretName(gitSecret)
                                          .build())
                          .build();
      return CIVolumeResponse.builder().volume(volume).volumeMount(volumeMount).build();
    }
    return null;
  }
}
