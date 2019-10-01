package io.harness.batch.processing.writer;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.EcsCCMConstants;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsTaskDescription;
import io.harness.event.payloads.EcsTaskInfo;
import io.harness.event.payloads.ReservedResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Qualifier("ecsTaskInfoWriter")
public class EcsTaskInfoWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is EcsTaskInfoWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_TASK_INFO))
        .forEach(publishedMessage -> {
          EcsTaskInfo ecsTaskInfo = (EcsTaskInfo) publishedMessage.getMessage();
          logger.debug("Message {} ", ecsTaskInfo);
          EcsTaskDescription ecsTaskDescription = ecsTaskInfo.getEcsTaskDescription();
          ReservedResource ecsTaskResource = ecsTaskInfo.getEcsTaskResource();
          String accountId = publishedMessage.getAccountId();
          String taskArn = ecsTaskDescription.getTaskArn();
          String clusterArn = ecsTaskDescription.getClusterArn();

          InstanceData instanceData = fetchActiveInstanceData(accountId, taskArn);
          InstanceType instanceType = getInstanceType(ecsTaskDescription);

          if (null == instanceData && null != instanceType) {
            Resource resource =
                Resource.builder().cpu(ecsTaskResource.getCpu()).memory(ecsTaskResource.getMemory()).build();

            Map<String, String> metaData = new HashMap<>();
            if (InstanceType.ECS_TASK_EC2 == instanceType) {
              InstanceData containerInstantData =
                  fetchInstanceData(accountId, ecsTaskDescription.getContainerInstanceArn());
              metaData.put(EcsCCMConstants.INSTANCE_FAMILY,
                  containerInstantData.getMetaData().get(EcsCCMConstants.INSTANCE_FAMILY));
              metaData.put(EcsCCMConstants.CONTAINER_INSTANCE_ARN, ecsTaskDescription.getContainerInstanceArn());
            }
            metaData.put(EcsCCMConstants.REGION, ecsTaskDescription.getRegion());
            String serviceName = !ecsTaskDescription.getServiceName().equals("")
                ? ecsTaskDescription.getServiceName()
                : EcsCCMConstants.CLUSTER_DEFAULT_SERVICE_NAME;
            metaData.put(EcsCCMConstants.ECS_SERVICE_NAME, serviceName);
            instanceData = InstanceData.builder()
                               .accountId(accountId)
                               .instanceId(taskArn)
                               .clusterName(clusterArn)
                               .instanceType(instanceType)
                               .instanceState(InstanceState.INITIALIZING)
                               .totalResource(resource)
                               .serviceName(serviceName)
                               .metaData(metaData)
                               .build();

            logger.info("Creating task {} ", taskArn);
            instanceDataService.create(instanceData);
          }
        });
  }

  private InstanceType getInstanceType(EcsTaskDescription ecsTaskDescription) {
    if (ecsTaskDescription.getLaunchType().equals(LaunchType.EC2.toString())) {
      return InstanceType.ECS_TASK_EC2;
    } else if (ecsTaskDescription.getLaunchType().equals(LaunchType.FARGATE.toString())) {
      return InstanceType.ECS_TASK_FARGATE;
    }
    return null;
  }
}
