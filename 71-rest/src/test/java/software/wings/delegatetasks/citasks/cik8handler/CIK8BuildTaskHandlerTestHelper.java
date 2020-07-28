package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static org.mockito.Mockito.mock;
import static software.wings.beans.ci.pod.SecretParams.Type.FILE;
import static software.wings.beans.ci.pod.SecretParams.Type.TEXT;
import static software.wings.delegatetasks.citasks.cik8handler.SecretSpecBuilder.SECRET_KEY;

import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import software.wings.beans.DockerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.SecretParams;
import software.wings.beans.container.ImageDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CIK8BuildTaskHandlerTestHelper {
  public static final String containerName2 = "container2";
  private static final String namespace = "default";
  private static final String podName = "pod";
  private static final String imageName = "IMAGE";
  private static final String containerName1 = "container1";
  private static final String tag = "TAG";
  private static final String registryUrl = "https://index.docker.io/v1/";

  public static CIK8BuildTaskParams buildGitSecretErrorTaskParams() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    GitConfig gitConfig = GitConfig.builder().build();

    GitFetchFilesConfig gitFetchFilesConfig =
        GitFetchFilesConfig.builder().gitConfig(gitConfig).encryptedDataDetails(encryptionDetails).build();
    CIK8PodParams<CIK8ContainerParams> cik8PodParams =
        CIK8PodParams.<CIK8ContainerParams>builder().name(podName).namespace(namespace).build();
    return CIK8BuildTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static CIK8BuildTaskParams buildImageSecretErrorTaskParams() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    GitConfig gitConfig = GitConfig.builder().build();
    GitFetchFilesConfig gitFetchFilesConfig =
        GitFetchFilesConfig.builder().gitConfig(gitConfig).encryptedDataDetails(encryptionDetails).build();
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    ImageDetails imageDetailsWithoutRegistry = ImageDetails.builder().name(imageName).tag(tag).build();

    List<CIK8ContainerParams> containerParamsList = new ArrayList<>();
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .imageDetailsWithConnector(ImageDetailsWithConnector.builder().imageDetails(imageDetails).build())
            .build());
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .imageDetailsWithConnector(
                ImageDetailsWithConnector.builder().imageDetails(imageDetailsWithoutRegistry).build())
            .build());

    CIK8PodParams<CIK8ContainerParams> cik8PodParams = CIK8PodParams.<CIK8ContainerParams>builder()
                                                           .name(podName)
                                                           .namespace(namespace)
                                                           .containerParamsList(containerParamsList)
                                                           .build();

    return CIK8BuildTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static CIK8BuildTaskParams buildPodCreateErrorTaskParams() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    GitConfig gitConfig = GitConfig.builder().build();
    GitFetchFilesConfig gitFetchFilesConfig =
        GitFetchFilesConfig.builder().gitConfig(gitConfig).encryptedDataDetails(encryptionDetails).build();
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    ImageDetails imageDetailsWithoutRegistry = ImageDetails.builder().name(imageName).tag(tag).build();

    List<CIK8ContainerParams> containerParamsList = new ArrayList<>();
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .imageDetailsWithConnector(ImageDetailsWithConnector.builder().imageDetails(imageDetails).build())
            .build());
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .imageDetailsWithConnector(
                ImageDetailsWithConnector.builder().imageDetails(imageDetailsWithoutRegistry).build())
            .build());

    CIK8PodParams<CIK8ContainerParams> cik8PodParams = CIK8PodParams.<CIK8ContainerParams>builder()
                                                           .name(podName)
                                                           .namespace(namespace)
                                                           .containerParamsList(containerParamsList)
                                                           .build();

    return CIK8BuildTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static CIK8BuildTaskParams buildTaskParams() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    GitConfig gitConfig = GitConfig.builder().build();
    GitFetchFilesConfig gitFetchFilesConfig =
        GitFetchFilesConfig.builder().gitConfig(gitConfig).encryptedDataDetails(encryptionDetails).build();
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    ImageDetails imageDetailsWithoutRegistry = ImageDetails.builder().name(imageName).tag(tag).build();

    List<CIK8ContainerParams> containerParamsList = new ArrayList<>();
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .name(containerName1)
            .containerType(CIContainerType.ADD_ON)
            .imageDetailsWithConnector(ImageDetailsWithConnector.builder().imageDetails(imageDetails).build())
            .containerSecrets(
                ContainerSecrets.builder().publishArtifactEncryptedValues(getPublishArtifactSettings()).build())
            .build());
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .containerSecrets(ContainerSecrets.builder().encryptedSecrets(getEncryptedDetails()).build())
            .name(containerName2)
            .containerType(CIContainerType.STEP_EXECUTOR)
            .imageDetailsWithConnector(
                ImageDetailsWithConnector.builder().imageDetails(imageDetailsWithoutRegistry).build())
            .build());

    CIK8PodParams<CIK8ContainerParams> cik8PodParams = CIK8PodParams.<CIK8ContainerParams>builder()
                                                           .name(podName)
                                                           .namespace(namespace)
                                                           .containerParamsList(containerParamsList)
                                                           .build();

    return CIK8BuildTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static Map<String, EncryptableSettingWithEncryptionDetails> getPublishArtifactSettings() {
    Map<String, EncryptableSettingWithEncryptionDetails> map = new HashMap<>();
    map.putAll(getDockerSettingWithEncryptionDetails());
    map.putAll(getGCPSettingWithEncryptionDetails());
    return map;
  }

  public static Map<String, EncryptableSettingWithEncryptionDetails> getDockerSettingWithEncryptionDetails() {
    Map<String, EncryptableSettingWithEncryptionDetails> encryptedSettings = new HashMap<>();

    EncryptableSettingWithEncryptionDetails encryptedDataDetail =
        EncryptableSettingWithEncryptionDetails.builder()
            .encryptedDataDetails(Collections.singletonList(
                EncryptedDataDetail.builder()
                    .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
                    .encryptionConfig(KmsConfig.builder()
                                          .accessKey("accessKey")
                                          .region("us-east-1")
                                          .secretKey("secretKey")
                                          .kmsArn("kmsArn")
                                          .build())
                    .build()))
            .encryptableSetting(DockerConfig.builder()
                                    .dockerRegistryUrl("https://index.docker.io/v1/")
                                    .username("uName")
                                    .password("pWord".toCharArray())
                                    .encryptedPassword("*****")
                                    .accountId("acctId")
                                    .build())
            .build();

    encryptedSettings.put("docker", encryptedDataDetail);
    return encryptedSettings;
  }

  public static Map<String, EncryptableSettingWithEncryptionDetails> getGCPSettingWithEncryptionDetails() {
    Map<String, EncryptableSettingWithEncryptionDetails> encryptedSettings = new HashMap<>();

    EncryptableSettingWithEncryptionDetails encryptedDataDetail =
        EncryptableSettingWithEncryptionDetails.builder()
            .encryptedDataDetails(Collections.singletonList(
                EncryptedDataDetail.builder()
                    .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
                    .encryptionConfig(KmsConfig.builder()
                                          .accessKey("accessKey")
                                          .region("us-east-1")
                                          .secretKey("secretKey")
                                          .kmsArn("kmsArn")
                                          .build())
                    .build()))
            .encryptableSetting(GcpConfig.builder().encryptedServiceAccountKeyFileContent("****").build())
            .build();

    encryptedSettings.put("gcp", encryptedDataDetail);
    return encryptedSettings;
  }

  public static Map<String, EncryptedVariableWithType> getEncryptedDetails() {
    Map<String, EncryptedVariableWithType> encryptedVariables = new HashMap<>();

    EncryptedVariableWithType encryptedVariableWithType =
        EncryptedVariableWithType.builder()
            .type(EncryptedVariableWithType.Type.TEXT)
            .encryptedDataDetail(
                EncryptedDataDetail.builder()
                    .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
                    .encryptionConfig(KmsConfig.builder()
                                          .accessKey("accessKey")
                                          .region("us-east-1")
                                          .secretKey("secretKey")
                                          .kmsArn("kmsArn")
                                          .build())
                    .build())
            .build();

    encryptedVariables.put("abc", encryptedVariableWithType);
    return encryptedVariables;
  }

  public static Map<String, SecretParams> getCustomVarSecret() {
    Map<String, SecretParams> decryptedSecrets = new HashMap<>();
    decryptedSecrets.put("docker",
        SecretParams.builder().type(TEXT).secretKey(SECRET_KEY + "docker").value(encodeBase64("pass")).build());
    return decryptedSecrets;
  }

  public static Map<String, SecretParams> getGcpSecret() {
    Map<String, SecretParams> decryptedSecrets = new HashMap<>();
    decryptedSecrets.put("SECRET_PATH_gcp",
        SecretParams.builder().type(FILE).secretKey("SECRET_PATH_gcp").value(encodeBase64("configFile:{}")).build());
    return decryptedSecrets;
  }
  public static Map<String, SecretParams> getDockerSecret() {
    Map<String, SecretParams> decryptedSecrets = new HashMap<>();
    decryptedSecrets.put("USERNAME_docker",
        SecretParams.builder().type(TEXT).secretKey("USERNAME_docker").value(encodeBase64("uname")).build());
    decryptedSecrets.put("PASSWORD_docker",
        SecretParams.builder().type(TEXT).secretKey("PASSWORD_docker").value(encodeBase64("passw")).build());
    decryptedSecrets.put("ENDPOINT_docker",
        SecretParams.builder().type(TEXT).secretKey("ENDPOINT_docker").value(encodeBase64("endpoint")).build());
    return decryptedSecrets;
  }
  public static Map<String, SecretParams> getPublishArtifactSecrets() {
    Map<String, SecretParams> decryptedSecrets = new HashMap<>();
    decryptedSecrets.putAll(getDockerSecret());
    decryptedSecrets.putAll(getGcpSecret());
    return decryptedSecrets;
  }
}
