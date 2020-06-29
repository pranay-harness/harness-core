package io.harness.batch.processing.processor;

import com.google.common.annotations.VisibleForTesting;

import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.processor.util.InstanceMetaDataUtils;
import io.harness.batch.processing.processor.util.K8sResourceUtils;
import io.harness.batch.processing.service.intfc.CloudProviderService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class K8sNodeInfoProcessor implements ItemProcessor<PublishedMessage, InstanceInfo> {
  @Autowired private InstanceDataService instanceDataService;
  @Autowired private CloudProviderService cloudProviderService;
  @Autowired private InstanceResourceService instanceResourceService;

  private static final String AWS_SPOT_INSTANCE = "spot";
  private static final String AZURE_SPOT_INSTANCE = "spot";

  @Override
  public InstanceInfo process(PublishedMessage publishedMessage) {
    NodeInfo nodeInfo = (NodeInfo) publishedMessage.getMessage();
    String accountId = publishedMessage.getAccountId();
    String clusterId = nodeInfo.getClusterId();
    String nodeUid = nodeInfo.getNodeUid();

    InstanceData existingInstanceData = instanceDataService.fetchInstanceData(accountId, clusterId, nodeUid);
    if (null != existingInstanceData) {
      return InstanceInfo.builder().metaData(Collections.emptyMap()).build();
    }

    Map<String, String> labelsMap = nodeInfo.getLabelsMap();
    Map<String, String> metaData = new HashMap<>();
    CloudProvider k8SCloudProvider =
        cloudProviderService.getK8SCloudProvider(nodeInfo.getCloudProviderId(), nodeInfo.getProviderId());
    String cloudProviderInstanceId = getCloudProviderInstanceId(nodeInfo.getProviderId());
    if (CloudProvider.UNKNOWN == k8SCloudProvider) {
      return InstanceInfo.builder().metaData(metaData).build();
    }
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, k8SCloudProvider.name());
    metaData.put(InstanceMetaDataConstants.REGION, labelsMap.get(K8sCCMConstants.REGION));
    metaData.put(InstanceMetaDataConstants.ZONE, labelsMap.get(K8sCCMConstants.ZONE));
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    metaData.put(InstanceMetaDataConstants.NODE_NAME, nodeInfo.getNodeName());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, labelsMap.get(K8sCCMConstants.INSTANCE_FAMILY));
    metaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM, labelsMap.get(K8sCCMConstants.OPERATING_SYSTEM));
    metaData.put(InstanceMetaDataConstants.NODE_UID, nodeUid);
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, cloudProviderInstanceId);
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, getInstanceCategory(k8SCloudProvider, labelsMap).name());
    if (null != labelsMap.get(K8sCCMConstants.COMPUTE_TYPE)) {
      metaData.put(InstanceMetaDataConstants.COMPUTE_TYPE, labelsMap.get(K8sCCMConstants.COMPUTE_TYPE));
    }

    Resource allocatableResource = K8sResourceUtils.getResource(nodeInfo.getAllocatableResourceMap());
    Resource totalResource = allocatableResource;
    List<CloudProvider> cloudProviders = cloudProviderService.getFirstClassSupportedCloudProviders();
    String computeType =
        InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.COMPUTE_TYPE, metaData);
    if (cloudProviders.contains(k8SCloudProvider) && !K8sCCMConstants.AWS_FARGATE_COMPUTE_TYPE.equals(computeType)) {
      totalResource = instanceResourceService.getComputeVMResource(
          InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.INSTANCE_FAMILY, metaData),
          InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.REGION, metaData),
          k8SCloudProvider);
    }

    return InstanceInfo.builder()
        .accountId(accountId)
        .settingId(nodeInfo.getCloudProviderId())
        .instanceId(nodeUid)
        .clusterId(clusterId)
        .cloudProviderInstanceId(cloudProviderInstanceId)
        .clusterName(nodeInfo.getClusterName())
        .instanceName(nodeInfo.getNodeName())
        .instanceType(InstanceType.K8S_NODE)
        .instanceState(InstanceState.INITIALIZING)
        .resource(totalResource)
        .allocatableResource(allocatableResource)
        .labels(nodeInfo.getLabelsMap())
        .metaData(metaData)
        .build();
  }

  @VisibleForTesting
  public InstanceCategory getInstanceCategory(CloudProvider k8SCloudProvider, Map<String, String> labelsMap) {
    InstanceCategory instanceCategory = InstanceCategory.ON_DEMAND;
    if (k8SCloudProvider == CloudProvider.GCP) {
      boolean preemptible = labelsMap.keySet().stream().anyMatch(key -> key.contains(K8sCCMConstants.PREEMPTIBLE_KEY));
      if (preemptible) {
        return InstanceCategory.SPOT;
      }
    } else if (k8SCloudProvider == CloudProvider.AWS) {
      List<String> lifecycleKeys = labelsMap.keySet()
                                       .stream()
                                       .filter(key -> key.contains(K8sCCMConstants.AWS_LIFECYCLE_KEY))
                                       .collect(Collectors.toList());
      for (String lifecycleKey : lifecycleKeys) {
        String lifecycle = labelsMap.get(lifecycleKey);
        if (lifecycle.toLowerCase().contains(AWS_SPOT_INSTANCE)) {
          return InstanceCategory.SPOT;
        }
      }
    } else if (k8SCloudProvider == CloudProvider.AZURE) {
      String lifecycle = labelsMap.get(K8sCCMConstants.AZURE_LIFECYCLE_KEY);
      if (null != lifecycle && lifecycle.toLowerCase().contains(AZURE_SPOT_INSTANCE)) {
        return InstanceCategory.SPOT;
      }
    }
    return instanceCategory;
  }

  @VisibleForTesting
  public String getCloudProviderInstanceId(String providerId) {
    return providerId.substring(providerId.lastIndexOf('/') + 1);
  }
}
