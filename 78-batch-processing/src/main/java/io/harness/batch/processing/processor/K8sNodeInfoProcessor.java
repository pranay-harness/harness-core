package io.harness.batch.processing.processor;

import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("k8sNodeInfoProcessor")
public class K8sNodeInfoProcessor implements ItemProcessor<PublishedMessage, InstanceInfo> {
  @Override
  public InstanceInfo process(PublishedMessage publishedMessage) {
    NodeInfo nodeInfo = (NodeInfo) publishedMessage.getMessage();

    return InstanceInfo.builder()
        .accountId(nodeInfo.getAccountId())
        .cloudProviderId(nodeInfo.getCloudProviderId())
        .instanceId(nodeInfo.getNodeUid())
        .instanceType(InstanceType.K8S_NODE)
        .labels(nodeInfo.getLabelsMap())
        // TODO: add more fields
        .build();
  }
}
