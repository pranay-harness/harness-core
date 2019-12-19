package io.harness.batch.processing.processor;

import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.processor.util.K8sResourceUtils;
import io.harness.batch.processing.service.intfc.CloudProviderService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class K8sNodeInfoProcessor implements ItemProcessor<PublishedMessage, InstanceInfo> {
  @Autowired private CloudProviderService cloudProviderService;

  @Override
  public InstanceInfo process(PublishedMessage publishedMessage) {
    NodeInfo nodeInfo = (NodeInfo) publishedMessage.getMessage();

    Map<String, String> labelsMap = nodeInfo.getLabelsMap();
    Map<String, String> metaData = new HashMap<>();
    CloudProvider k8SCloudProvider =
        cloudProviderService.getK8SCloudProvider(nodeInfo.getCloudProviderId(), nodeInfo.getProviderId());
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, k8SCloudProvider.name());
    metaData.put(InstanceMetaDataConstants.REGION, labelsMap.get(K8sCCMConstants.REGION));
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    metaData.put(InstanceMetaDataConstants.NODE_NAME, nodeInfo.getNodeName());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, labelsMap.get(K8sCCMConstants.INSTANCE_FAMILY));
    metaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM, labelsMap.get(K8sCCMConstants.OPERATING_SYSTEM));
    metaData.put(InstanceMetaDataConstants.NODE_UID, nodeInfo.getNodeUid());

    return InstanceInfo.builder()
        .accountId(publishedMessage.getAccountId())
        .settingId(nodeInfo.getCloudProviderId())
        .instanceId(nodeInfo.getNodeUid())
        .clusterId(nodeInfo.getClusterId())
        .clusterName(nodeInfo.getClusterName())
        .instanceName(nodeInfo.getNodeName())
        .instanceType(InstanceType.K8S_NODE)
        .instanceState(InstanceState.INITIALIZING)
        .resource(K8sResourceUtils.getResource(nodeInfo.getAllocatableResourceMap()))
        .labels(nodeInfo.getLabelsMap())
        .metaData(metaData)
        .build();
  }
}
