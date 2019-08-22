package io.harness.batch.processing.writer.constants;

public interface EventTypeConstants {
  String EC2_INSTANCE_INFO = "io.harness.event.payloads.Ec2InstanceInfo";
  String EC2_INSTANCE_LIFECYCLE = "io.harness.event.payloads.Ec2Lifecycle";
  String ECS_CONTAINER_INSTANCE_INFO = "io.harness.event.payloads.EcsContainerInstanceInfo";
  String ECS_CONTAINER_INSTANCE_LIFECYCLE = "io.harness.event.payloads.EcsContainerInstanceLifecycle";
  String ECS_TASK_INFO = "io.harness.event.payloads.EcsTaskInfo";
  String ECS_TASK_LIFECYCLE = "io.harness.event.payloads.EcsTaskLifecycle";
  String ECS_SYNC_EVENT = "io.harness.event.payloads.EcsSyncEvent";
}
