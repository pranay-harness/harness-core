package io.harness.delegate.task.citasks.cik8handler.pod;

import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.HostAliasParams;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.task.citasks.cik8handler.container.ContainerSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.container.ContainerSpecBuilderResponse;

import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HostAlias;
import io.fabric8.kubernetes.api.model.HostAliasBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodFluent;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract class to generate K8 pod spec based on parameters provided to it. It builds a minimal pod spec essential
 * for creating a pod. This class can be extended to generate a generic pod spec.
 */
public abstract class BasePodSpecBuilder {
  @Inject private ContainerSpecBuilder containerSpecBuilder;

  public PodBuilder createSpec(PodParams<ContainerParams> podParams) {
    PodFluent.SpecNested<PodBuilder> podBuilderSpecNested = getBaseSpec(podParams);
    decorateSpec(podParams, podBuilderSpecNested);
    return podBuilderSpecNested.endSpec();
  }

  /**
   * Builds on minimal pod spec generated by getBaseSpec method.
   */
  protected abstract void decorateSpec(
      PodParams<ContainerParams> podParams, PodFluent.SpecNested<PodBuilder> podBuilderSpecNested);

  private PodFluent.SpecNested<PodBuilder> getBaseSpec(PodParams<ContainerParams> podParams) {
    List<LocalObjectReference> imageSecrets = new ArrayList<>();

    Set<Volume> volumesToCreate = new HashSet<>();
    Map<String, String> volumeToPVCMap = getPVC(podParams.getPvcParamList());
    Map<String, LocalObjectReference> imageSecretByName = new HashMap<>();
    List<Container> containers =
        getContainers(podParams.getContainerParamsList(), volumesToCreate, volumeToPVCMap, imageSecretByName);
    List<Container> initContainers =
        getContainers(podParams.getInitContainerParamsList(), volumesToCreate, volumeToPVCMap, imageSecretByName);

    imageSecretByName.forEach((imageName, imageSecret) -> imageSecrets.add(imageSecret));

    return new PodBuilder()
        .withNewMetadata()
        .withName(podParams.getName())
        .withLabels(podParams.getLabels())
        .withNamespace(podParams.getNamespace())
        .endMetadata()
        .withNewSpec()
        .withContainers(containers)
        .withInitContainers(initContainers)
        .withImagePullSecrets(imageSecrets)
        .withHostAliases(getHostAliases(podParams.getHostAliasParamsList()))
        .withVolumes(new ArrayList<>(volumesToCreate));
  }

  private List<Container> getContainers(List<ContainerParams> containerParamsList, Set<Volume> volumesToCreate,
      Map<String, String> volumeToPVCMap, Map<String, LocalObjectReference> imageSecretByName) {
    List<Container> containers = new ArrayList<>();
    if (containerParamsList == null) {
      return containers;
    }

    for (ContainerParams containerParams : containerParamsList) {
      if (containerParams.getVolumeToMountPath() != null) {
        containerParams.getVolumeToMountPath().forEach(
            (volumeName, volumeMountPath) -> volumesToCreate.add(getVolume(volumeName, volumeToPVCMap)));
      }

      ContainerSpecBuilderResponse containerSpecBuilderResponse = containerSpecBuilder.createSpec(containerParams);
      containers.add(containerSpecBuilderResponse.getContainerBuilder().build());
      if (containerSpecBuilderResponse.getImageSecret() != null) {
        LocalObjectReference imageSecret = containerSpecBuilderResponse.getImageSecret();
        imageSecretByName.put(imageSecret.getName(), imageSecret);
      }
      if (containerSpecBuilderResponse.getVolumes() != null) {
        List<Volume> volumes = containerSpecBuilderResponse.getVolumes();
        volumesToCreate.addAll(volumes);
      }
    }
    return containers;
  }

  private Map<String, String> getPVC(List<PVCParams> pvcParamsList) {
    Map<String, String> volumeToPVCMap = new HashMap<>();
    if (pvcParamsList == null) {
      return volumeToPVCMap;
    }

    for (PVCParams pvcParam : pvcParamsList) {
      volumeToPVCMap.put(pvcParam.getVolumeName(), pvcParam.getClaimName());
    }
    return volumeToPVCMap;
  }

  private Volume getVolume(String volumeName, Map<String, String> volumeToPVCMap) {
    if (volumeToPVCMap.containsKey(volumeName)) {
      return new VolumeBuilder()
          .withName(volumeName)
          .withPersistentVolumeClaim(
              new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(volumeToPVCMap.get(volumeName)).build())
          .build();
    } else {
      return new VolumeBuilder().withName(volumeName).withEmptyDir(new EmptyDirVolumeSourceBuilder().build()).build();
    }
  }

  private List<HostAlias> getHostAliases(List<HostAliasParams> hostAliasParamsList) {
    List<HostAlias> hostAliases = new ArrayList<>();
    if (hostAliasParamsList == null) {
      return hostAliases;
    }

    hostAliasParamsList.forEach(hostAliasParams
        -> hostAliases.add(new HostAliasBuilder()
                               .withHostnames(hostAliasParams.getHostnameList())
                               .withIp(hostAliasParams.getIpAddress())
                               .build()));
    return hostAliases;
  }
}
