package io.harness.delegate.task.citasks.cik8handler.container;

import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_CLONE_CONTAINER_NAME;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_CLONE_IMAGE_NAME;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_CLONE_IMAGE_TAG;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_PASS_ENV_VAR;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_SSH_VOL_DEFAULT_MODE;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_SSH_VOL_MOUNT_PATH;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_SSH_VOL_NAME;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.GIT_USERNAME_ENV_VAR;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.delegate.task.citasks.cik8handler.params.CIGitConstants.LOG_SERVICE_ENDPOINT_VARIABLE_VALUE;

import static java.lang.String.format;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.task.citasks.cik8handler.SecretSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.params.GitCloneContainerParams;
import io.harness.delegate.task.citasks.cik8handler.params.GitCloneContainerParams.GitCloneContainerParamsBuilder;
import io.harness.encryption.SecretRefData;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GitCloneContainerSpecBuilderTestHelper {
  private static final String stepExecVolumeName = "step-exec";
  private static final String stepExecWorkingDir = "workspace";
  private static final String volumeMountPath = "/harness-" + stepExecVolumeName;

  private static final String gitPwdRepoUrl = "https://github.com/wings-software/portal.git";
  private static final String gitInvalidRepoUrl = "xhttps://github.com/wings-software/portal.git";
  private static final String gitRepoInCmd = "github.com/wings-software/portal.git";
  private static final String gitSshRepoUrl = "git@github.com:wings-software/portal.git";

  private static final String gitUsername = "username";
  private static final String gitPasswordRefId = "git_password";
  private static final String gitSshKey = "git_ssh_key";

  private static final String gitBranch = "master";
  private static final String gitCommitId = "commit";
  private static final String gitSecretName = "hs-wings-software-portal-hs";

  private static List<String> gitCtrCommands = Arrays.asList("/bin/sh", "-c", "--");

  public static GitCloneContainerParams emptyGitCloneParams() {
    return GitCloneContainerParams.builder().build();
  }

  public static GitCloneContainerParams gitCloneParamsWithUnsetGitConfigParams() {
    return GitCloneContainerParams.builder().gitConnectorDetails(ConnectorDetails.builder().build()).build();
  }

  private static GitCloneContainerParamsBuilder getCompleteGitParamsBuilder() {
    return GitCloneContainerParams.builder()
        .workingDir(stepExecWorkingDir)
        .branchName(gitBranch)
        .stepExecVolumeName(stepExecVolumeName)
        .gitConnectorDetails(
            ConnectorDetails
                .builder()

                .connectorType(ConnectorType.GIT)
                .connectorConfig(
                    GitConfigDTO.builder()
                        .gitAuthType(GitAuthType.HTTP)
                        .gitAuth(GitHTTPAuthenticationDTO.builder()
                                     .username(gitUsername)
                                     .passwordRef(SecretRefData.builder().identifier(gitPasswordRefId).build())
                                     .build())
                        .branchName(gitBranch)
                        .url(gitPwdRepoUrl)
                        .build())
                .build());
  }

  public static GitCloneContainerParams gitCloneParamsWithUnsetGitRepoUrl() {
    return getCompleteGitParamsBuilder()
        .gitConnectorDetails(
            ConnectorDetails.builder()
                .connectorType(ConnectorType.GIT)
                .connectorConfig(
                    GitConfigDTO.builder()
                        .gitAuthType(GitAuthType.HTTP)
                        .gitAuth(GitHTTPAuthenticationDTO.builder()
                                     .username(gitUsername)
                                     .passwordRef(SecretRefData.builder().identifier(gitPasswordRefId).build())
                                     .build())
                        .branchName(gitBranch)
                        .build())
                .build())
        .build();
  }

  public static GitCloneContainerParams gitCloneParamsWithUnsetBranch() {
    return getCompleteGitParamsBuilder()
        .branchName(null)
        .gitConnectorDetails(
            ConnectorDetails
                .builder()

                .connectorType(ConnectorType.GIT)
                .connectorConfig(GitConfigDTO.builder()
                                     .gitAuthType(GitAuthType.SSH)
                                     .gitAuth(GitSSHAuthenticationDTO.builder().encryptedSshKey(gitSshKey).build())
                                     .url(gitSshRepoUrl)
                                     .build())
                .build())
        .build();
  }

  public static GitCloneContainerParams gitCloneParamsWithUnsetWorkDir() {
    return getCompleteGitParamsBuilder().workingDir(null).build();
  }

  public static GitCloneContainerParams gitCloneParamsWithUnsetVolume() {
    return getCompleteGitParamsBuilder().stepExecVolumeName(null).build();
  }

  public static GitCloneContainerParams gitCloneParamsWithInvalidAuth() {
    return getCompleteGitParamsBuilder()
        .gitConnectorDetails(
            ConnectorDetails
                .builder()

                .connectorType(ConnectorType.GIT)
                .connectorConfig(
                    GitConfigDTO.builder()
                        .gitAuthType(GitAuthType.HTTP)
                        .gitAuth(GitHTTPAuthenticationDTO.builder()
                                     .username(gitUsername)
                                     .passwordRef(SecretRefData.builder().identifier(gitPasswordRefId).build())
                                     .build())
                        .branchName(gitBranch)
                        .url(gitInvalidRepoUrl)
                        .build())
                .build())
        .build();
  }

  public static GitCloneContainerParams gitCloneParamsWithPassword() {
    return getCompleteGitParamsBuilder().build();
  }

  public static GitCloneContainerParams gitCloneParamsWithSsh() {
    return GitCloneContainerParams.builder()
        .workingDir(stepExecWorkingDir)
        .branchName(gitBranch)
        .stepExecVolumeName(stepExecVolumeName)
        .gitConnectorDetails(
            ConnectorDetails
                .builder()

                .connectorType(ConnectorType.GIT)
                .connectorConfig(GitConfigDTO.builder()
                                     .gitAuthType(GitAuthType.SSH)
                                     .gitAuth(GitSSHAuthenticationDTO.builder().encryptedSshKey(gitSshKey).build())
                                     .branchName(gitBranch)
                                     .url(gitSshRepoUrl)
                                     .build())
                .build())
        .build();
  }

  public static GitCloneContainerParams gitCloneParamsWithSshAndCommit() {
    return getCompleteGitParamsBuilder()
        .commitId(gitCommitId)
        .gitConnectorDetails(
            ConnectorDetails.builder()
                .connectorType(ConnectorType.GIT)
                .connectorConfig(GitConfigDTO.builder()
                                     .gitAuthType(GitAuthType.SSH)
                                     .gitAuth(GitSSHAuthenticationDTO.builder().encryptedSshKey(gitSshKey).build())
                                     .branchName(gitBranch)
                                     .url(gitSshRepoUrl)
                                     .build())
                .build())
        .build();
  }

  public static Container gitCloneWithPwdExpectedResponse() {
    String gitCtrArgs = format("git clone -b %s --single-branch -- https://$(GIT_USERNAME):$(GIT_PASSWORD)@%s %s/%s",
        gitBranch, gitRepoInCmd, volumeMountPath, stepExecWorkingDir);

    EnvVarSource gitUserNameSource = new EnvVarSourceBuilder()
                                         .withSecretKeyRef(new SecretKeySelectorBuilder()
                                                               .withName(gitSecretName)
                                                               .withKey(SecretSpecBuilder.GIT_SECRET_USERNAME_KEY)
                                                               .build())
                                         .build();
    EnvVarSource gitPwdSource = new EnvVarSourceBuilder()
                                    .withSecretKeyRef(new SecretKeySelectorBuilder()
                                                          .withName(gitSecretName)
                                                          .withKey(SecretSpecBuilder.GIT_SECRET_PWD_KEY)
                                                          .build())
                                    .build();
    List<EnvVar> envVars = new ArrayList<>();
    envVars.add(new EnvVarBuilder().withName(GIT_USERNAME_ENV_VAR).withValueFrom(gitUserNameSource).build());
    envVars.add(new EnvVarBuilder().withName(GIT_PASS_ENV_VAR).withValueFrom(gitPwdSource).build());
    envVars.add(new EnvVarBuilder()
                    .withName(LOG_SERVICE_ENDPOINT_VARIABLE)
                    .withValue(LOG_SERVICE_ENDPOINT_VARIABLE_VALUE)
                    .build());

    return new ContainerBuilder()
        .withCommand(gitCtrCommands)
        .withArgs(gitCtrArgs)
        .withImage(GIT_CLONE_IMAGE_NAME + ":" + GIT_CLONE_IMAGE_TAG)
        .withName(GIT_CLONE_CONTAINER_NAME)
        .withSecurityContext(new SecurityContextBuilder().withPrivileged(false).build())
        .withEnv(envVars)
        .withVolumeMounts(new VolumeMountBuilder().withName(stepExecVolumeName).withMountPath(volumeMountPath).build())
        .build();
  }

  public static Container gitCloneWithSSHExpectedResponse() {
    String gitCtrArgs = format(
        "GIT_SSH_COMMAND='ssh -i /etc/git-secret/ssh_key -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no' git clone -b %s --single-branch -- %s %s/%s",
        gitBranch, gitSshRepoUrl, volumeMountPath, stepExecWorkingDir);
    List<VolumeMount> volumeMounts = new ArrayList<>();
    volumeMounts.add(new VolumeMountBuilder().withName(stepExecVolumeName).withMountPath(volumeMountPath).build());
    volumeMounts.add(new VolumeMountBuilder().withName(GIT_SSH_VOL_NAME).withMountPath(GIT_SSH_VOL_MOUNT_PATH).build());
    List<EnvVar> envVars = new ArrayList<>();
    envVars.add(new EnvVarBuilder()
                    .withName(LOG_SERVICE_ENDPOINT_VARIABLE)
                    .withValue(LOG_SERVICE_ENDPOINT_VARIABLE_VALUE)
                    .build());

    return new ContainerBuilder()
        .withCommand(gitCtrCommands)
        .withArgs(gitCtrArgs)
        .withImage(GIT_CLONE_IMAGE_NAME + ":" + GIT_CLONE_IMAGE_TAG)
        .withName(GIT_CLONE_CONTAINER_NAME)
        .withSecurityContext(new SecurityContextBuilder().withPrivileged(false).build())
        .withEnv(envVars)
        .withVolumeMounts(volumeMounts)
        .build();
  }

  public static Container gitCloneWithSSHAndCommitExpectedResponse() {
    String gitCtrArgs = format(
        "GIT_SSH_COMMAND='ssh -i /etc/git-secret/ssh_key -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no' git clone -b %s --single-branch -- %s %s/%s; cd %s/%s; git checkout %s",
        gitBranch, gitSshRepoUrl, volumeMountPath, stepExecWorkingDir, volumeMountPath, stepExecWorkingDir,
        gitCommitId);
    List<VolumeMount> volumeMounts = new ArrayList<>();
    volumeMounts.add(new VolumeMountBuilder().withName(stepExecVolumeName).withMountPath(volumeMountPath).build());
    volumeMounts.add(new VolumeMountBuilder().withName(GIT_SSH_VOL_NAME).withMountPath(GIT_SSH_VOL_MOUNT_PATH).build());
    List<EnvVar> envVars = new ArrayList<>();
    envVars.add(new EnvVarBuilder()
                    .withName(LOG_SERVICE_ENDPOINT_VARIABLE)
                    .withValue(LOG_SERVICE_ENDPOINT_VARIABLE_VALUE)
                    .build());

    return new ContainerBuilder()
        .withCommand(gitCtrCommands)
        .withArgs(gitCtrArgs)
        .withImage(GIT_CLONE_IMAGE_NAME + ":" + GIT_CLONE_IMAGE_TAG)
        .withName(GIT_CLONE_CONTAINER_NAME)
        .withSecurityContext(new SecurityContextBuilder().withPrivileged(false).build())
        .withEnv(envVars)
        .withVolumeMounts(volumeMounts)
        .build();
  }

  public static List<Volume> gitCloneWithSSHExpectedVolumes() {
    List<Volume> expectedVolumes = new ArrayList<>();
    Volume volume = new VolumeBuilder()
                        .withName(GIT_SSH_VOL_NAME)
                        .withSecret(new SecretVolumeSourceBuilder()
                                        .withDefaultMode(GIT_SSH_VOL_DEFAULT_MODE)
                                        .withSecretName(gitSecretName)
                                        .build())
                        .build();
    expectedVolumes.add(volume);
    return expectedVolumes;
  }
}
