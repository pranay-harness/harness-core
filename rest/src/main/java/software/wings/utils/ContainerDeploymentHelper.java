package software.wings.utils;

import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import io.harness.data.structure.EmptyPredicate;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/6/18.
 */
public class ContainerDeploymentHelper {
  public static List<InstanceStatusSummary> getInstanceStatusSummaryFromContainerInfoList(
      List<ContainerInfo> containerInfos, ServiceTemplateElement serviceTemplateElement) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(containerInfos)) {
      for (ContainerInfo containerInfo : containerInfos) {
        HostElement hostElement = aHostElement()
                                      .withHostName(containerInfo.getHostName())
                                      .withEc2Instance(containerInfo.getEc2Instance())
                                      .build();
        InstanceElement instanceElement = anInstanceElement()
                                              .withUuid(containerInfo.getContainerId())
                                              .withDockerId(containerInfo.getContainerId())
                                              .withHostName(containerInfo.getHostName())
                                              .withHost(hostElement)
                                              .withServiceTemplateElement(serviceTemplateElement)
                                              .withDisplayName(containerInfo.getContainerId())
                                              .build();
        ExecutionStatus status =
            containerInfo.getStatus() == Status.SUCCESS ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
        instanceStatusSummaries.add(
            anInstanceStatusSummary().withStatus(status).withInstanceElement(instanceElement).build());
      }
    }
    return instanceStatusSummaries;
  }
}
