package io.harness.delegate.task.citasks.cik8handler.pod;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ContainerSecrets;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDTO;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.task.citasks.cik8handler.params.CIConstants;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.k8s.model.ImageDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CIK8PodSpecBuilderTestHelper {
  private static String stepExecVolumeName = "step-exec";
  private static String stepExecWorkingDir = "workspace";
  private static String podName = "pod";
  private static String namespace = "default";

  private static String keyLabel1 = "hello";
  private static String valLabel1 = "world";
  private static String keyLab1l2 = "foo";
  private static String valLabel2 = "bar";

  private static String containerName1 = "container1";
  private static String containerName2 = "container2";
  private static String volume1 = "volume1";
  private static String mountPath1 = "/mnt1";
  private static String volume2 = "volume2";
  private static String mountPath2 = "/mnt2";

  private static String imageName = "IMAGE";
  private static String tag = "TAG";
  private static String imageCtrName = imageName + ":" + tag;
  private static String registryUrl = "https://index.docker.io/v1/";
  private static String userName = "usr";
  private static String password = "pwd";
  private static String registrySecretName = "hs-index-docker-io-v1-usr-hs";
  private static String storageClass = "test-storage";
  private static Integer storageMib = 100;
  private static String claimName = "pvc";

  public static CIK8ContainerParams basicContainerParamsWithoutImageCred() {
    ImageDetails imageWithoutCred = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    return CIK8ContainerParams.builder()
        .name(containerName1)
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder().imageDetails(imageWithoutCred).build())
        .build();
  }

  public static CIK8ContainerParams containerParamsWithSecretEnvVar() {
    SecretVariableDetails secretVariableDetails =
        SecretVariableDetails.builder()
            .secretVariableDTO(SecretVariableDTO.builder()
                                   .name("variable_name")
                                   .type(SecretVariableDTO.Type.TEXT)
                                   .secret(SecretRefData.builder().identifier("secret_id").scope(Scope.ACCOUNT).build())
                                   .build())
            .encryptedDataDetailList(singletonList(
                EncryptedDataDetail.builder()
                    .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
                    .build()))
            .build();

    return CIK8ContainerParams.builder()
        .containerSecrets(
            ContainerSecrets.builder().secretVariableDetails(singletonList(secretVariableDetails)).build())
        .name(containerName1)
        .build();
  }

  public static CIK8ContainerParams basicContainerParamsWithImageCred() {
    ImageDetails imageDetailsWithCred = ImageDetails.builder()
                                            .name(imageName)
                                            .tag(tag)
                                            .registryUrl(registryUrl)
                                            .username(userName)
                                            .password(password)
                                            .build();
    return CIK8ContainerParams.builder()
        .name(containerName1)
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder().imageDetails(imageDetailsWithCred).build())
        .build();
  }
  public static CIK8ContainerParams containerParamsWithVoluemMount() {
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(volume1, mountPath1);
    ImageDetails imageDetailsWithCred = ImageDetails.builder()
                                            .name(imageName)
                                            .tag(tag)
                                            .registryUrl(registryUrl)
                                            .username(userName)
                                            .password(password)
                                            .build();
    return CIK8ContainerParams.builder()
        .name(containerName1)
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder().imageDetails(imageDetailsWithCred).build())
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  public static PVCParams pvcParams(String volumeName) {
    return PVCParams.builder()
        .volumeName(volumeName)
        .claimName(claimName)
        .storageClass(storageClass)
        .isPresent(false)
        .sizeMib(storageMib)
        .build();
  }

  public static ContainerBuilder basicContainerBuilder() {
    return new ContainerBuilder().withName(containerName1).withImage(imageCtrName);
  }

  public static ContainerBuilder containerBuilderWithVolumeMount() {
    List<VolumeMount> ctrVolumeMounts = new ArrayList<>();
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume1).withMountPath(mountPath1).build());
    return new ContainerBuilder().withName(containerName1).withImage(imageCtrName).withVolumeMounts(ctrVolumeMounts);
  }

  public static ContainerBuilder gitCloneCtrBuilder() {
    return new ContainerBuilder().withName(containerName2).withImage(imageCtrName);
  }

  public static CIK8PodParams<CIK8ContainerParams> basicInput() {
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podName)
        .namespace(namespace)
        .stepExecVolumeName(stepExecVolumeName)
        .stepExecWorkingDir(stepExecWorkingDir)
        .containerParamsList(asList(basicContainerParamsWithoutImageCred()))
        .build();
  }

  public static CIK8PodParams<CIK8ContainerParams> getPodSpecWithEnvSecret() {
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podName)
        .namespace(namespace)
        .stepExecVolumeName(stepExecVolumeName)
        .stepExecWorkingDir(stepExecWorkingDir)
        .containerParamsList(asList(containerParamsWithSecretEnvVar()))
        .build();
  }

  public static CIK8PodParams<CIK8ContainerParams> basicInputWithImageCred() {
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podName)
        .namespace(namespace)
        .stepExecVolumeName(stepExecVolumeName)
        .stepExecWorkingDir(stepExecWorkingDir)
        .containerParamsList(asList(basicContainerParamsWithImageCred()))
        .build();
  }
  public static CIK8PodParams<CIK8ContainerParams> basicInputWithVolumeMount() {
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podName)
        .namespace(namespace)
        .stepExecVolumeName(stepExecVolumeName)
        .stepExecWorkingDir(stepExecWorkingDir)
        .containerParamsList(asList(containerParamsWithVoluemMount()))
        .build();
  }
  public static CIK8PodParams<CIK8ContainerParams> basicInputWithPVC() {
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podName)
        .namespace(namespace)
        .stepExecVolumeName(stepExecVolumeName)
        .stepExecWorkingDir(stepExecWorkingDir)
        .containerParamsList(asList(containerParamsWithVoluemMount()))
        .pvcParamList(asList(pvcParams(volume1)))
        .build();
  }

  public static Pod basicExpectedPod() {
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .endSpec()
        .build();
  }

  public static Pod basicExpectedPodWithImageCred() {
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withImagePullSecrets(new LocalObjectReference(registrySecretName))
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .endSpec()
        .build();
  }

  public static Pod basicExpectedPodWithVolumeMount() {
    List<Volume> volumes = new ArrayList<>();
    volumes.add(new VolumeBuilder().withName(volume1).withEmptyDir(new EmptyDirVolumeSourceBuilder().build()).build());
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(containerBuilderWithVolumeMount().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withImagePullSecrets(new LocalObjectReference(registrySecretName))
        .withVolumes(volumes)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .endSpec()
        .build();
  }

  public static Pod basicExpectedPodWithPVC() {
    List<Volume> volumes = new ArrayList<>();
    volumes.add(
        new VolumeBuilder()
            .withName(volume1)
            .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(claimName).build())
            .build());
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(containerBuilderWithVolumeMount().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withImagePullSecrets(new LocalObjectReference(registrySecretName))
        .withVolumes(volumes)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .endSpec()
        .build();
  }

  public static Pod expectedPodWithInitContainer() {
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withInitContainers(gitCloneCtrBuilder().build())
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .endSpec()
        .build();
  }

  public static Pod expectedPodWithInitContainerAndVolume() {
    return new PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withInitContainers(gitCloneCtrBuilder().build())
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withVolumes(new Volume())
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .endSpec()
        .build();
  }
}
