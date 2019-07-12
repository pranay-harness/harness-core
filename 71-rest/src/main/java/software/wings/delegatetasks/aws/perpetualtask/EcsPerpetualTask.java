package software.wings.delegatetasks.aws.perpetualtask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.ContainerInstanceStatus;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.Resource;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.Task;
import io.harness.event.PublishMessage;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.Ec2InstanceInfo;
import io.harness.event.payloads.Ec2Lifecycle;
import io.harness.event.payloads.EcsContainerInstanceDescription;
import io.harness.event.payloads.EcsContainerInstanceInfo;
import io.harness.event.payloads.EcsContainerInstanceLifecycle;
import io.harness.event.payloads.EcsTaskDescription;
import io.harness.event.payloads.EcsTaskInfo;
import io.harness.event.payloads.EcsTaskLifecycle;
import io.harness.event.payloads.InstanceState;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.event.payloads.ReservedResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import software.wings.service.impl.aws.model.AwsEcsListClusterServicesRequest;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class EcsPerpetualTask {
  @Inject private AwsEcsHelperServiceDelegate ecsHelperServiceDelegate;
  @Inject private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Inject EventPublisher eventPublisher;

  private static final String INSTANCE_TERMINATED_NAME = "terminated";

  public void run(AwsEcsListClusterServicesRequest awsEcsListClusterServicesRequest) {
    logger.info("ECS Perpetual cluster service request");
    List<ContainerInstance> containerInstances = listContainerInstances(awsEcsListClusterServicesRequest);
    List<Instance> instances = listEc2Instances(awsEcsListClusterServicesRequest, containerInstances);

    Map<String, String> taskArnServiceNameMap = new HashMap<>();
    loadTaskArnServiceNameMap(awsEcsListClusterServicesRequest, taskArnServiceNameMap);
    List<Task> tasks = listTask(awsEcsListClusterServicesRequest);

    publishEc2InstanceEvent(instances);
    publishContainerInstanceEvent(containerInstances);
    publishTaskEvent(tasks, taskArnServiceNameMap);
  }

  private void publishTaskEvent(List<Task> tasks, Map<String, String> taskArnServiceNameMap) {
    logger.info("Task size is {} ", tasks.size());
    for (Task task : tasks) {
      int memory = Integer.valueOf(task.getMemory());
      int cpu = Integer.valueOf(task.getCpu());

      if (null != task.getPullStartedAt()) {
        publishTaskLifecycleEvent(task.getTaskArn(), task.getPullStartedAt(), EventType.START);
      }

      if (null != task.getStoppedAt()) {
        publishTaskLifecycleEvent(task.getTaskArn(), task.getStoppedAt(), EventType.STOP);
      }

      EcsTaskInfo.Builder ecsTaskInfoBuilder = EcsTaskInfo.newBuilder()
                                                   .setTaskArn(task.getTaskArn())
                                                   .setLaunchType(task.getLaunchType())
                                                   .setClusterArn(task.getClusterArn())
                                                   .setDesiredStatus(task.getDesiredStatus());

      if (null != taskArnServiceNameMap.get(task.getTaskArn())) {
        ecsTaskInfoBuilder.setServiceName(taskArnServiceNameMap.get(task.getTaskArn()));
      }

      if (null != task.getContainerInstanceArn()) {
        ecsTaskInfoBuilder.setContainerInstanceArn(task.getContainerInstanceArn());
      }

      PublishMessage publishMessage =
          PublishMessage.newBuilder()
              .setPayload(
                  Any.pack(EcsTaskDescription.newBuilder()
                               .setEcsTaskInfo(ecsTaskInfoBuilder.build())
                               .setEcsTaskResource(ReservedResource.newBuilder().setCpu(cpu).setMemory(memory).build())
                               .build()))
              .build();
      logger.info("Task published Message {} ", publishMessage.toString());
      eventPublisher.publish(publishMessage);
    }
  }

  private Lifecycle createLifecycle(String instanceId, Date date, EventType eventType) {
    return Lifecycle.newBuilder()
        .setInstanceId(instanceId)
        .setTimestamp(convertDateToTimestamp(date))
        .setType(eventType)
        .setCreatedTimestamp(convertDateToTimestamp(Date.from(Instant.now())))
        .build();
  }

  private void publishEc2LifecycleEvent(String instanceId, Date date, EventType eventType) {
    Ec2Lifecycle ec2Lifecycle =
        Ec2Lifecycle.newBuilder().setLifecycle(createLifecycle(instanceId, date, eventType)).build();
    PublishMessage publishMessage = PublishMessage.newBuilder().setPayload(Any.pack(ec2Lifecycle)).build();
    eventPublisher.publish(publishMessage);
  }

  private void publishContainerInstanceLifecycleEvent(String instanceId, Date date, EventType eventType) {
    EcsContainerInstanceLifecycle ecsContainerInstanceLifecycle =
        EcsContainerInstanceLifecycle.newBuilder().setLifecycle(createLifecycle(instanceId, date, eventType)).build();
    PublishMessage publishMessage =
        PublishMessage.newBuilder().setPayload(Any.pack(ecsContainerInstanceLifecycle)).build();
    eventPublisher.publish(publishMessage);
  }

  private void publishTaskLifecycleEvent(String instanceId, Date date, EventType eventType) {
    EcsTaskLifecycle ecsTaskLifecycle =
        EcsTaskLifecycle.newBuilder().setLifecycle(createLifecycle(instanceId, date, eventType)).build();
    PublishMessage publishMessage = PublishMessage.newBuilder().setPayload(Any.pack(ecsTaskLifecycle)).build();
    eventPublisher.publish(publishMessage);
  }

  private void publishContainerInstanceEvent(List<ContainerInstance> containerInstances) {
    logger.info("Container instance size is {} ", containerInstances.size());
    for (ContainerInstance containerInstance : containerInstances) {
      publishContainerInstanceLifecycleEvent(
          containerInstance.getContainerInstanceArn(), containerInstance.getRegisteredAt(), EventType.START);

      List<Resource> registeredResources = containerInstance.getRegisteredResources();
      Map<String, Resource> resourceMap =
          registeredResources.stream().collect(Collectors.toMap(Resource::getName, resource -> resource));

      int memory = resourceMap.get("MEMORY").getIntegerValue();
      int cpu = resourceMap.get("CPU").getIntegerValue();

      PublishMessage publishMessage =
          PublishMessage.newBuilder()
              .setPayload(Any.pack(EcsContainerInstanceDescription.newBuilder()
                                       .setEcsContainerInstanceInfo(
                                           EcsContainerInstanceInfo.newBuilder()
                                               .setClusterArn(containerInstance.getContainerInstanceArn())
                                               .setContainerInstanceArn(containerInstance.getContainerInstanceArn())
                                               .setEc2InstanceId(containerInstance.getEc2InstanceId())
                                               .build())
                                       .setEcsContainerInstanceResource(
                                           ReservedResource.newBuilder().setCpu(cpu).setMemory(memory).build())
                                       .build()))
              .build();
      logger.info("Container published Message {} ", publishMessage.toString());
      eventPublisher.publish(publishMessage);
    }
  }

  private void publishEc2InstanceEvent(List<Instance> instances) {
    logger.info("Instance list size is {} ", instances.size());
    for (Instance instance : instances) {
      if (null != instance.getLaunchTime()) {
        publishEc2LifecycleEvent(instance.getInstanceId(), instance.getLaunchTime(), EventType.START);
      }

      InstanceState.Builder instanceStateBuilder =
          InstanceState.newBuilder().setCode(instance.getState().getCode()).setName(instance.getState().getName());

      if (INSTANCE_TERMINATED_NAME.equals(instance.getState().getName())) {
        publishEc2LifecycleEvent(instance.getInstanceId(), Date.from(Instant.now()), EventType.STOP);
      }

      Ec2InstanceInfo.Builder ec2InstanceInfoBuilder = Ec2InstanceInfo.newBuilder()
                                                           .setInstanceId(instance.getInstanceId())
                                                           .setInstanceType(instance.getInstanceType())
                                                           .setInstanceState(instanceStateBuilder.build());

      if (null != instance.getCapacityReservationId()) {
        ec2InstanceInfoBuilder.setCapacityReservationId(instance.getCapacityReservationId());
      }

      if (null != instance.getSpotInstanceRequestId()) {
        ec2InstanceInfoBuilder.setSpotInstanceRequestId(instance.getSpotInstanceRequestId());
      }

      if (null != instance.getInstanceLifecycle()) {
        ec2InstanceInfoBuilder.setInstanceLifecycle(instance.getInstanceLifecycle());
      }

      PublishMessage publishMessage =
          PublishMessage.newBuilder().setPayload(Any.pack(ec2InstanceInfoBuilder.build())).build();
      logger.info("EC2 published Message {} ", publishMessage.toString());
      eventPublisher.publish(publishMessage);
    }
  }

  private List<Task> listTask(AwsEcsListClusterServicesRequest request) {
    List<DesiredStatus> desiredStatuses = listTaskDesiredStatus();
    List<Task> tasks = new ArrayList<>();
    for (DesiredStatus desiredStatus : desiredStatuses) {
      tasks.addAll(ecsHelperServiceDelegate.listTasksForService(request.getAwsConfig(), request.getEncryptionDetails(),
          request.getRegion(), request.getCluster(), null, desiredStatus));
    }
    return tasks;
  }

  private List<DesiredStatus> listTaskDesiredStatus() {
    return new ArrayList<>(Arrays.asList(DesiredStatus.RUNNING, DesiredStatus.STOPPED));
  }

  private List<ContainerInstanceStatus> listContainerInstanceStatus() {
    return new ArrayList<>(Arrays.asList(ContainerInstanceStatus.ACTIVE, ContainerInstanceStatus.DRAINING));
  }

  private void loadTaskArnServiceNameMap(
      AwsEcsListClusterServicesRequest request, Map<String, String> taskArnServiceNameMap) {
    List<DesiredStatus> desiredStatuses = listTaskDesiredStatus();
    for (Service service : listServices(request)) {
      for (DesiredStatus desiredStatus : desiredStatuses) {
        List<String> taskArns =
            ecsHelperServiceDelegate.listTasksArnForService(request.getAwsConfig(), request.getEncryptionDetails(),
                request.getRegion(), request.getCluster(), service.getServiceArn(), desiredStatus);
        if (!CollectionUtils.isEmpty(taskArns)) {
          for (String taskArn : taskArns) {
            taskArnServiceNameMap.put(taskArn, service.getServiceArn());
          }
        }
      }
    }
  }

  private List<Service> listServices(AwsEcsListClusterServicesRequest request) {
    return ecsHelperServiceDelegate.listServicesForCluster(
        request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion(), request.getCluster());
  }

  private List<ContainerInstance> listContainerInstances(
      AwsEcsListClusterServicesRequest awsEcsListClusterServicesRequest) {
    List<ContainerInstanceStatus> containerInstanceStatuses = listContainerInstanceStatus();
    List<ContainerInstance> containerInstances = new ArrayList<>();
    for (ContainerInstanceStatus containerInstanceStatus : containerInstanceStatuses) {
      containerInstances.addAll(
          ecsHelperServiceDelegate.listContainerInstancesForCluster(awsEcsListClusterServicesRequest.getAwsConfig(),
              awsEcsListClusterServicesRequest.getEncryptionDetails(), awsEcsListClusterServicesRequest.getRegion(),
              awsEcsListClusterServicesRequest.getCluster(), containerInstanceStatus));
    }
    return containerInstances;
  }

  private List<Instance> listEc2Instances(
      AwsEcsListClusterServicesRequest request, List<ContainerInstance> containerInstances) {
    List<Instance> instances = new ArrayList<>();
    if (!CollectionUtils.isEmpty(containerInstances)) {
      List<String> instanceIds = containerInstances.stream()
                                     .map(containerInstance -> containerInstance.getEc2InstanceId())
                                     .collect(Collectors.toList());
      instances = ec2ServiceDelegate.listEc2Instances(
          request.getAwsConfig(), request.getEncryptionDetails(), instanceIds, request.getRegion());
      logger.info("Instances {} ", instances.toString());
    }
    return instances;
  }

  private Timestamp convertDateToTimestamp(Date date) {
    return Timestamp.newBuilder()
        .setSeconds(date.getTime() / 1000)
        .setNanos((int) ((date.getTime() % 1000) * 1000000))
        .build();
  }
}
