package software.wings.delegatetasks.citasks.cik8handler;

import static org.mockito.Mockito.mock;

import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.container.ImageDetails;

import java.util.ArrayList;
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
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    GitConfig gitConfig = GitConfig.builder().build();

    GitFetchFilesConfig gitFetchFilesConfig =
        GitFetchFilesConfig.builder().gitConfig(gitConfig).encryptedDataDetails(encryptionDetails).build();
    CIK8PodParams<CIK8ContainerParams> cik8PodParams =
        CIK8PodParams.<CIK8ContainerParams>builder().name(podName).namespace(namespace).build();
    return CIK8BuildTaskParams.builder()
        .kubernetesConfig(kubernetesConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static CIK8BuildTaskParams buildImageSecretErrorTaskParams() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
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
        .kubernetesConfig(kubernetesConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static CIK8BuildTaskParams buildPodCreateErrorTaskParams() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
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
        .kubernetesConfig(kubernetesConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static CIK8BuildTaskParams buildTaskParams() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
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
            .build());
    containerParamsList.add(
        CIK8ContainerParams.builder()
            .encryptedSecrets(getEncryptedDetails())
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
        .kubernetesConfig(kubernetesConfig)
        .encryptionDetails(encryptionDetails)
        .gitFetchFilesConfig(gitFetchFilesConfig)
        .cik8PodParams(cik8PodParams)
        .build();
  }

  public static Map<String, EncryptedDataDetail> getEncryptedDetails() {
    Map<String, EncryptedDataDetail> encryptedVariables = new HashMap<>();

    EncryptedDataDetail encryptedDataDetail =
        EncryptedDataDetail.builder()
            .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
            .encryptionConfig(KmsConfig.builder()
                                  .accessKey("accessKey")
                                  .region("us-east-1")
                                  .secretKey("secretKey")
                                  .kmsArn("kmsArn")
                                  .build())
            .build();

    encryptedVariables.put("abc", encryptedDataDetail);
    return encryptedVariables;
  }
}
