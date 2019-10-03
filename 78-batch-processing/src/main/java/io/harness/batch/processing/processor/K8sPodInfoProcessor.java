package io.harness.batch.processing.processor;

import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class K8sPodInfoProcessor implements ItemProcessor<PublishedMessage, InstanceInfo> {
  @Override
  public InstanceInfo process(PublishedMessage publishedMessage) {
    PodInfo podInfo = (PodInfo) publishedMessage.getMessage();

    return InstanceInfo.builder()
        .accountId(podInfo.getAccountId())
        .cloudProviderId(podInfo.getCloudProviderId())
        .instanceId(podInfo.getPodUid())
        .instanceType(InstanceType.K8S_POD)
        .resource(from(podInfo.getTotalResource()))
        //.containerList(podInfo.getContainersList())
        .labels(podInfo.getLabelsMap())
        // TODO: add missing fields in PodInfo
        .build();
  }

  private Resource from(io.harness.perpetualtask.k8s.watch.Resource resource) {
    Quantity cpuQuantity = resource.getRequestsMap().get("cpu");
    Quantity memQuantity = resource.getRequestsMap().get("memory");

    return Resource.builder()
        .cpu(Double.valueOf(cpuQuantity.getAmount()))
        .cpuUnit(cpuQuantity.getUnit())
        .memory(Double.valueOf(memQuantity.getAmount()))
        .memoryUnit(memQuantity.getUnit())
        .build();
  }
}
