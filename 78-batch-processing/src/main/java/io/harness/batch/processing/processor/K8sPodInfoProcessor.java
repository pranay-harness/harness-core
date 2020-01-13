package io.harness.batch.processing.processor;

import com.google.common.annotations.VisibleForTesting;

import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.processor.util.K8sResourceUtils;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.api.DeploymentSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class K8sPodInfoProcessor implements ItemProcessor<PublishedMessage, InstanceInfo> {
  @Autowired private InstanceDataService instanceDataService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private WorkloadRepository workloadRepository;
  private static String POD = "Pod";

  @Override
  public InstanceInfo process(PublishedMessage publishedMessage) {
    String accountId = publishedMessage.getAccountId();
    PodInfo podInfo = (PodInfo) publishedMessage.getMessage();
    String workloadName = podInfo.getTopLevelOwner().getName();
    String workloadType = podInfo.getTopLevelOwner().getKind();

    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.GCP.name());
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, podInfo.getNodeName());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    metaData.put(InstanceMetaDataConstants.NAMESPACE, podInfo.getNamespace());
    metaData.put(
        InstanceMetaDataConstants.WORKLOAD_NAME, workloadName.equals("") ? podInfo.getPodName() : workloadName);
    metaData.put(InstanceMetaDataConstants.WORKLOAD_TYPE, workloadType.equals("") ? POD : workloadType);

    InstanceData instanceData = instanceDataService.fetchInstanceDataWithName(
        accountId, podInfo.getCloudProviderId(), podInfo.getNodeName(), publishedMessage.getOccurredAt());
    if (null != instanceData) {
      Map<String, String> nodeMetaData = instanceData.getMetaData();
      metaData.put(InstanceMetaDataConstants.REGION, nodeMetaData.get(InstanceMetaDataConstants.REGION));
      metaData.put(InstanceMetaDataConstants.ZONE, nodeMetaData.get(InstanceMetaDataConstants.ZONE));
      metaData.put(
          InstanceMetaDataConstants.INSTANCE_FAMILY, nodeMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY));
      metaData.put(
          InstanceMetaDataConstants.INSTANCE_CATEGORY, nodeMetaData.get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
      metaData.put(
          InstanceMetaDataConstants.CLOUD_PROVIDER, nodeMetaData.get(InstanceMetaDataConstants.CLOUD_PROVIDER));
      metaData.put(
          InstanceMetaDataConstants.OPERATING_SYSTEM, nodeMetaData.get(InstanceMetaDataConstants.OPERATING_SYSTEM));
      metaData.put(InstanceMetaDataConstants.POD_NAME, podInfo.getPodName());
      metaData.put(
          InstanceMetaDataConstants.PARENT_RESOURCE_CPU, String.valueOf(instanceData.getTotalResource().getCpuUnits()));
      metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY,
          String.valueOf(instanceData.getTotalResource().getMemoryMb()));
    } else {
      logger.error(
          "Node detail not found settingId {} node name {}", podInfo.getCloudProviderId(), podInfo.getNodeName());
    }

    Map<String, String> labelsMap = podInfo.getLabelsMap();
    HarnessServiceInfo harnessServiceInfo = getHarnessServiceInfo(accountId, labelsMap);

    try {
      workloadRepository.savePodWorkload(accountId, podInfo);
    } catch (Exception ex) {
      logger.error("Error while saving pod workload {} {}", podInfo.getCloudProviderId(), podInfo.getPodUid());
    }

    return InstanceInfo.builder()
        .accountId(accountId)
        .settingId(podInfo.getCloudProviderId())
        .instanceId(podInfo.getPodUid())
        .clusterId(podInfo.getClusterId())
        .clusterName(podInfo.getClusterName())
        .instanceName(podInfo.getPodName())
        .instanceType(InstanceType.K8S_POD)
        .instanceState(InstanceState.INITIALIZING)
        .resource(K8sResourceUtils.getResource(podInfo.getTotalResource().getRequestsMap()))
        .metaData(metaData)
        //.containerList(podInfo.getContainersList())
        .labels(labelsMap)
        .harnessServiceInfo(harnessServiceInfo)
        // TODO: add missing fields in PodInfo
        .build();
  }

  @VisibleForTesting
  HarnessServiceInfo getHarnessServiceInfo(String accountId, Map<String, String> labelsMap) {
    if (labelsMap.containsKey(K8sCCMConstants.RELEASE_NAME)) {
      String releaseName = labelsMap.get(K8sCCMConstants.RELEASE_NAME);
      K8sDeploymentKey k8sDeploymentKey = K8sDeploymentKey.builder().releaseName(releaseName).build();
      K8sDeploymentInfo k8sDeploymentInfo = K8sDeploymentInfo.builder().releaseName(releaseName).build();
      DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                                .accountId(accountId)
                                                .k8sDeploymentKey(k8sDeploymentKey)
                                                .deploymentInfo(k8sDeploymentInfo)
                                                .build();
      Optional<HarnessServiceInfo> harnessServiceInfo =
          cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummary);
      return harnessServiceInfo.orElse(null);
    }
    return null;
  }
}
