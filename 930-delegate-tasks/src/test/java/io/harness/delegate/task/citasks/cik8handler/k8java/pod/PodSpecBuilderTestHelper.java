/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler.k8java.pod;

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

import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1EmptyDirVolumeSourceBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReferenceBuilder;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSourceBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PodSpecBuilderTestHelper {
  private static String stepExecVolumeName = "step-exec";
  private static String stepExecWorkingDir = "workspace";
  private static String podName = "pod";
  private static String namespace = "default";

  private static String containerName1 = "container1";
  private static String containerName2 = "container2";
  private static String volume1 = "volume1";
  private static String mountPath1 = "/mnt1";

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

  public static V1ContainerBuilder basicContainerBuilder() {
    return new V1ContainerBuilder().withName(containerName1).withImage(imageCtrName);
  }

  public static V1ContainerBuilder containerBuilderWithVolumeMount() {
    List<V1VolumeMount> ctrVolumeMounts = new ArrayList<>();
    ctrVolumeMounts.add(new V1VolumeMountBuilder().withName(volume1).withMountPath(mountPath1).build());
    return new V1ContainerBuilder().withName(containerName1).withImage(imageCtrName).withVolumeMounts(ctrVolumeMounts);
  }

  public static V1ContainerBuilder gitCloneCtrBuilder() {
    return new V1ContainerBuilder().withName(containerName2).withImage(imageCtrName);
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

  public static V1Pod basicExpectedPod() {
    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .withHostAliases(new ArrayList<>())
        .withVolumes(new ArrayList<>())
        .withInitContainers(new ArrayList<>())
        .withImagePullSecrets(new ArrayList<>())
        .endSpec()
        .build();
  }

  public static V1Pod basicExpectedPodWithImageCred() {
    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(registrySecretName).build())
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .withHostAliases(new ArrayList<>())
        .withVolumes(new ArrayList<>())
        .withInitContainers(new ArrayList<>())
        .endSpec()
        .build();
  }

  public static V1Pod basicExpectedPodWithVolumeMount() {
    List<V1Volume> volumes = new ArrayList<>();
    volumes.add(
        new V1VolumeBuilder().withName(volume1).withEmptyDir(new V1EmptyDirVolumeSourceBuilder().build()).build());
    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(containerBuilderWithVolumeMount().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(registrySecretName).build())
        .withVolumes(volumes)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .withHostAliases(new ArrayList<>())
        .withInitContainers(new ArrayList<>())
        .endSpec()
        .build();
  }

  public static V1Pod basicExpectedPodWithPVC() {
    List<V1Volume> volumes = new ArrayList<>();
    volumes.add(new V1VolumeBuilder()
                    .withName(volume1)
                    .withPersistentVolumeClaim(
                        new V1PersistentVolumeClaimVolumeSourceBuilder().withClaimName(claimName).build())
                    .build());
    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(containerBuilderWithVolumeMount().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withImagePullSecrets(new V1LocalObjectReferenceBuilder().withName(registrySecretName).build())
        .withVolumes(volumes)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .withHostAliases(new ArrayList<>())
        .withInitContainers(new ArrayList<>())
        .endSpec()
        .build();
  }

  public static V1Pod expectedPodWithInitContainer() {
    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .withHostAliases(new ArrayList<>())
        .withVolumes(new ArrayList<>())
        .withInitContainers(new ArrayList<>())
        .withImagePullSecrets(new ArrayList<>())
        .endSpec()
        .build();
  }

  public static V1Pod expectedPodWithInitContainerAndVolume() {
    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(basicContainerBuilder().build())
        .withRestartPolicy(CIConstants.RESTART_POLICY)
        .withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS)
        .withHostAliases(new ArrayList<>())
        .withVolumes(new ArrayList<>())
        .withInitContainers(new ArrayList<>())
        .withImagePullSecrets(new ArrayList<>())
        .endSpec()
        .build();
  }
}
