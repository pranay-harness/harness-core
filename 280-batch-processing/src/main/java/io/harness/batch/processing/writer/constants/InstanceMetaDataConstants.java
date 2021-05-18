package io.harness.batch.processing.writer.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CE)
public class InstanceMetaDataConstants {
  public static final String ZONE = "zone";
  public static final String REGION = "region";
  public static final String POD_NAME = "pod_name";
  public static final String NODE_UID = "node_uid";
  public static final String NODE_NAME = "node_name";
  public static final String NODE_POOL_NAME = "node_pool_name";
  public static final String TASK_ID = "task_id";
  public static final String LAUNCH_TYPE = "launch_type";
  public static final String NAMESPACE = "namespace";
  public static final String CLUSTER_ARN = "cluster_arn";
  public static final String COMPUTE_TYPE = "compute_type";
  public static final String CLUSTER_TYPE = "cluster_type";
  public static final String CLOUD_PROVIDER = "cloud_provider";
  public static final String WORKLOAD_ID = "workload_id";
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
  public static final String ACTUAL_PARENT_RESOURCE_ID = "actual_parent_resource_id";
  public static final String CLOUD_PROVIDER_INSTANCE_ID = "cloud_provider_instance_id";
  public static final String CLAIM_NAME = "claim_name";
  public static final String CLAIM_NAMESPACE = "claim_namespace";
  public static final String PV_TYPE = "pv_type";
  public static final String GCE_STORAGE_CLASS = "type";
  public static final String POD_CAPACITY = "pod_capacity";
  public static final String SLOW_ACCOUNT = "SFByhonVQvGJX0SbY82rjA";
  public static final String AZURE_SUBSCRIPTION_ID = "vm_subscription_id";
  public static final String AZURE_RESOURCEGROUP_NAME = "vm_resource_group_name";
  public static final int VM_INDEX = 2;
  public static final String VM_INDEX_VALUE = "virtualMachines";
  public static final int VMSS_INDEX = 4;
  public static final String VMSS_INDEX_VALUE = "virtualMachineScaleSets";
  public static final int SUBSCRIPTION_INDEX = 3;
  public static final String SUBSCRIPTION_INDEX_VALUE = "subscriptions";
  public static final int RESOURCE_GROUP_INDEX = 5;
  public static final String RESOURCE_GROUP_INDEX_VALUE = "resourceGroups";

  private InstanceMetaDataConstants() {}
}
