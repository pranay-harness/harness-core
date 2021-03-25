package software.wings.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.HasPredicate.hasSome;

import static software.wings.service.InstanceSyncConstants.CONTAINER_SERVICE_NAME;
import static software.wings.service.InstanceSyncConstants.CONTAINER_TYPE;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.NAMESPACE;
import static software.wings.service.InstanceSyncConstants.RELEASE_NAME;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;
import static software.wings.service.impl.ContainerMetadataType.K8S;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.data.structure.HasPredicate;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.BaseContainerDeploymentInfo;
import software.wings.api.ContainerDeploymentInfoWithLabels;
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
import software.wings.utils.Utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContainerInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject InstanceService instanceService;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject ContainerInstanceSyncPerpetualTaskClient containerInstanceSyncPerpetualTaskClient;

  static final boolean ALLOW_DUPLICATE = false;

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
    log.info("Found {} instances for app {}", instances.size(), appId);

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
                -> deploymentInfo instanceof BaseContainerDeploymentInfo || deploymentInfo instanceof K8sDeploymentInfo)
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
        .map(params -> create(accountId, params))
        .collect(Collectors.toList());
  }

  private String create(String accountId, ContainerInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(HARNESS_APPLICATION_ID, clientParams.getAppId());
    clientParamMap.put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId());
    clientParamMap.put(NAMESPACE, clientParams.getNamespace());
    clientParamMap.put(RELEASE_NAME, clientParams.getReleaseName());
    clientParamMap.put(CONTAINER_SERVICE_NAME, clientParams.getContainerSvcName());
    clientParamMap.put(CONTAINER_TYPE, Utils.emptyIfNull(clientParams.getContainerType()));

    PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        PerpetualTaskType.CONTAINER_INSTANCE_SYNC, accountId, clientContext, schedule, ALLOW_DUPLICATE, "");
  }

  private Set<ContainerMetadata> extractContainerMetadata(DeploymentInfo deploymentInfo) {
    return deploymentInfo instanceof BaseContainerDeploymentInfo
        ? getContainerMetadataFromContainerDeploymentInfo((BaseContainerDeploymentInfo) deploymentInfo)
        : getContainerMetadataFromK8DeploymentInfo((K8sDeploymentInfo) deploymentInfo);
  }

  private Set<ContainerMetadata> getContainerMetadataFromContainerDeploymentInfo(
      BaseContainerDeploymentInfo baseContainerDeploymentInfo) {
    if (baseContainerDeploymentInfo instanceof ContainerDeploymentInfoWithNames) {
      return ImmutableSet.of(
          ContainerMetadata.builder()
              .containerServiceName(
                  ((ContainerDeploymentInfoWithNames) baseContainerDeploymentInfo).getContainerSvcName())
              .namespace(((ContainerDeploymentInfoWithNames) baseContainerDeploymentInfo).getNamespace())
              .build());
    } else if (baseContainerDeploymentInfo instanceof ContainerDeploymentInfoWithLabels) {
      Set<String> controllers =
          emptyIfNull(((ContainerDeploymentInfoWithLabels) baseContainerDeploymentInfo).getContainerInfoList())
              .stream()
              .map(io.harness.container.ContainerInfo::getWorkloadName)
              .filter(HasPredicate::hasSome)
              .collect(Collectors.toSet());
      if (hasSome(controllers)) {
        return controllers.stream()
            .map(controller
                -> ContainerMetadata.builder()
                       .namespace(((ContainerDeploymentInfoWithLabels) baseContainerDeploymentInfo).getNamespace())
                       .containerServiceName(controller)
                       .releaseName(((ContainerDeploymentInfoWithLabels) baseContainerDeploymentInfo).getReleaseName())
                       .build())
            .collect(Collectors.toSet());
      } else if (hasSome(((ContainerDeploymentInfoWithLabels) baseContainerDeploymentInfo).getContainerInfoList())) {
        return ImmutableSet.of(
            ContainerMetadata.builder()
                .namespace(((ContainerDeploymentInfoWithLabels) baseContainerDeploymentInfo).getNamespace())
                .releaseName(((ContainerDeploymentInfoWithLabels) baseContainerDeploymentInfo).getReleaseName())
                .build());
      }
    }
    return Collections.emptySet();
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

    if (hasSome(k8sDeploymentInfo.getNamespaces())) {
      namespaces.addAll(k8sDeploymentInfo.getNamespaces());
    }

    return namespaces;
  }
}
