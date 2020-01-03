package io.harness.batch.processing.writer.constants;

public class InstanceMetaDataConstants {
  public static final String ZONE = "zone";
  public static final String REGION = "region";
  public static final String POD_NAME = "pod_name";
  public static final String NODE_UID = "node_uid";
  public static final String NODE_NAME = "node_name";
  public static final String TASK_ID = "task_id";
  public static final String LAUNCH_TYPE = "launch_type";
  public static final String NAMESPACE = "namespace";
  public static final String CLUSTER_TYPE = "cluster_type";
  public static final String CLOUD_PROVIDER = "cloud_provider";
  public static final String WORKLOAD_NAME = "workload_name";
  public static final String WORKLOAD_TYPE = "workload_type";
  public static final String INSTANCE_FAMILY = "instance_family";
  public static final String EC2_INSTANCE_ID = "ec2_instance_id";
  public static final String ECS_SERVICE_NAME = "ecs_service_name";
  public static final String ECS_SERVICE_ARN = "ecs_service_arn";
  public static final String OPERATING_SYSTEM = "operating_system";
  public static final String INSTANCE_CATEGORY = "instance_category";
  public static final String PARENT_RESOURCE_ID = "parent_resource_id";
  public static final String PARENT_RESOURCE_CPU = "parent_resource_cpu";
  public static final String PARENT_RESOURCE_MEMORY = "parent_resource_memory";
  public static final String CONTAINER_INSTANCE_ARN = "container_instance_arn";

  private InstanceMetaDataConstants() {}
}
