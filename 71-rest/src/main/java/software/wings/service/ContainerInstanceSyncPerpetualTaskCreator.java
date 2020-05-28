package software.wings.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.ContainerMetadataType.K8S;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;

import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;

import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.ContainerMetadataType;
import software.wings.service.intfc.instance.InstanceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContainerInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject InstanceService instanceService;
  @Inject ContainerInstanceSyncPerpetualTaskClient containerInstanceSyncPerpetualTaskClient;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    final String accountId = infrastructureMapping.getAccountId();
    final String appId = infrastructureMapping.getAppId();
    final String infraMappingId = infrastructureMapping.getUuid();
    Set<ContainerMetadata> containersMetadata = getContainerMetadataFromInstances(appId, infraMappingId);

    return createPerpetualTasks(containersMetadata, accountId, appId, infraMappingId);
  }

  private Set<ContainerMetadata> getContainerMetadataFromInstances(String appId, String infrastructureMappingId) {
    List<Instance> instances = instanceService.getInstancesForAppAndInframapping(appId, infrastructureMappingId);
    logger.info("Found {} instances for app {}", instances.size(), appId);

    return emptyIfNull(instances)
        .stream()
        .map(Instance::getInstanceInfo)
        .filter(ContainerInfo.class ::isInstance)
        .map(ContainerInfo.class ::cast)
        .filter(containerInfo
            -> containerInfo instanceof KubernetesContainerInfo || containerInfo instanceof K8sPodInfo
                || containerInfo instanceof EcsContainerInfo)
        .map(containerInfo
            -> ContainerMetadata.builder()
                   .type(getContainerMetadataTypeFrom(containerInfo))
                   .containerServiceName(getContainerSvcNameIfAvailable(containerInfo))
                   .namespace(getNamespaceIfAvailable(containerInfo))
                   .releaseName(getReleaseNameIfAvailable(containerInfo))
                   .build())
        .collect(Collectors.toSet());
  }

  private String getNamespaceIfAvailable(ContainerInfo containerInfo) {
    return !(containerInfo instanceof EcsContainerInfo) ? getNamespace(containerInfo) : null;
  }

  private String getNamespace(ContainerInfo containerInfo) {
    return containerInfo instanceof KubernetesContainerInfo ? ((KubernetesContainerInfo) containerInfo).getNamespace()
                                                            : ((K8sPodInfo) containerInfo).getNamespace();
  }

  private String getReleaseNameIfAvailable(ContainerInfo containerInfo) {
    return containerInfo instanceof K8sPodInfo ? ((K8sPodInfo) containerInfo).getReleaseName() : null;
  }

  private ContainerMetadataType getContainerMetadataTypeFrom(ContainerInfo containerInfo) {
    return containerInfo instanceof K8sPodInfo ? K8S : null;
  }

  private String getContainerSvcNameIfAvailable(ContainerInfo containerInfo) {
    return !(containerInfo instanceof K8sPodInfo) ? getContainerSvcName(containerInfo) : null;
  }

  private String getContainerSvcName(ContainerInfo containerInfo) {
    return containerInfo instanceof KubernetesContainerInfo
        ? ((KubernetesContainerInfo) containerInfo).getControllerName()
        : ((EcsContainerInfo) containerInfo).getServiceName();
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    String appId = deploymentSummaries.iterator().next().getAppId();
    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    String accountId = deploymentSummaries.iterator().next().getAccountId();

    Set<ContainerMetadata> existingContainersMetadata =
        existingPerpetualTasks.stream()
            .map(record
                -> ContainerMetadata.builder()
                       .containerServiceName(record.getClientContext().getClientParams().get(
                           InstanceSyncConstants.CONTAINER_SERVICE_NAME))
                       .namespace(record.getClientContext().getClientParams().get(InstanceSyncConstants.NAMESPACE))
                       .releaseName(record.getClientContext().getClientParams().get(InstanceSyncConstants.RELEASE_NAME))
                       .type(extractContainerMetadataType(
                           record.getClientContext().getClientParams().get(InstanceSyncConstants.CONTAINER_TYPE)))
                       .build())
            .collect(Collectors.toSet());

    Set<ContainerMetadata> newDeploymentContainersMetadata =
        deploymentSummaries.stream()
            .map(DeploymentSummary::getDeploymentInfo)
            .filter(deploymentInfo
                -> deploymentInfo instanceof ContainerDeploymentInfoWithNames
                    || deploymentInfo instanceof K8sDeploymentInfo)
            .flatMap(deploymentInfo -> extractContainerMetadata(deploymentInfo).stream())
            .collect(Collectors.toSet());

    SetView<ContainerMetadata> containersMetadataToExamine =
        Sets.difference(newDeploymentContainersMetadata, existingContainersMetadata);

    return createPerpetualTasks(containersMetadataToExamine, accountId, appId, infraMappingId);
  }

  private ContainerMetadataType extractContainerMetadataType(String containerTypeRecord) {
    return K8S.name().equals(containerTypeRecord) ? K8S : null;
  }

  private List<String> createPerpetualTasks(
      Set<ContainerMetadata> containersMetadata, String accountId, String appId, String infraMappingId) {
    return containersMetadata.stream()
        .map(containerMetadata
            -> ContainerInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(appId)
                   .inframappingId(infraMappingId)
                   .containerSvcName(containerMetadata.getContainerServiceName())
                   .namespace(containerMetadata.getNamespace())
                   .releaseName(containerMetadata.getReleaseName())
                   .containerType(nonNull(containerMetadata.getType()) ? containerMetadata.getType().name() : null)
                   .build())
        .map(params -> containerInstanceSyncPerpetualTaskClient.create(accountId, params))
        .collect(Collectors.toList());
  }

  private Set<ContainerMetadata> extractContainerMetadata(DeploymentInfo deploymentInfo) {
    return deploymentInfo instanceof ContainerDeploymentInfoWithNames
        ? getContainerMetadataFromDeploymentInfoWithNames((ContainerDeploymentInfoWithNames) deploymentInfo)
        : getContainerMetadataFromK8DeploymentInfo((K8sDeploymentInfo) deploymentInfo);
  }

  private Set<ContainerMetadata> getContainerMetadataFromDeploymentInfoWithNames(
      ContainerDeploymentInfoWithNames containerDeploymentInfoWithNames) {
    return ImmutableSet.of(ContainerMetadata.builder()
                               .containerServiceName(containerDeploymentInfoWithNames.getContainerSvcName())
                               .namespace(containerDeploymentInfoWithNames.getNamespace())
                               .build());
  }

  private Set<ContainerMetadata> getContainerMetadataFromK8DeploymentInfo(K8sDeploymentInfo k8sDeploymentInfo) {
    return getNamespaces(k8sDeploymentInfo)
        .stream()
        .map(namespace
            -> ContainerMetadata.builder()
                   .type(K8S)
                   .releaseName(k8sDeploymentInfo.getReleaseName())
                   .namespace(namespace)
                   .build())
        .collect(Collectors.toSet());
  }

  private Set<String> getNamespaces(K8sDeploymentInfo k8sDeploymentInfo) {
    Set<String> namespaces = new HashSet<>();
    if (isNotBlank(k8sDeploymentInfo.getNamespace())) {
      namespaces.add(k8sDeploymentInfo.getNamespace());
    }

    if (isNotEmpty(k8sDeploymentInfo.getNamespaces())) {
      namespaces.addAll(k8sDeploymentInfo.getNamespaces());
    }

    return namespaces;
  }
}
