package io.harness.batch.processing.writer.constants;

import io.harness.event.payloads.ContainerStateProto;
import io.harness.event.payloads.Ec2InstanceInfo;
import io.harness.event.payloads.Ec2Lifecycle;
import io.harness.event.payloads.EcsContainerInstanceInfo;
import io.harness.event.payloads.EcsContainerInstanceLifecycle;
import io.harness.event.payloads.EcsSyncEvent;
import io.harness.event.payloads.EcsTaskInfo;
import io.harness.event.payloads.EcsTaskLifecycle;
import io.harness.event.payloads.EcsUtilization;
import io.harness.event.payloads.NodeMetric;
import io.harness.event.payloads.PodMetric;
import io.harness.perpetualtask.k8s.watch.K8SClusterSyncEvent;
import io.harness.perpetualtask.k8s.watch.K8sWatchEvent;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.harness.perpetualtask.k8s.watch.NodeEvent;
import io.harness.perpetualtask.k8s.watch.NodeInfo;
import io.harness.perpetualtask.k8s.watch.PodEvent;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventTypeConstants {
  public final String K8S_POD_INFO = PodInfo.class.getName();
  public final String K8S_NODE_INFO = NodeInfo.class.getName();
  public final String K8S_POD_EVENT = PodEvent.class.getName();
  public final String K8S_NODE_EVENT = NodeEvent.class.getName();
  public final String ECS_TASK_INFO = EcsTaskInfo.class.getName();
  public final String POD_UTILIZATION = PodMetric.class.getName();
  public final String NODE_UTILIZATION = NodeMetric.class.getName();
  public final String ECS_SYNC_EVENT = EcsSyncEvent.class.getName();
  public final String ECS_UTILIZATION = EcsUtilization.class.getName();
  public final String EC2_INSTANCE_INFO = Ec2InstanceInfo.class.getName();
  public final String K8S_SYNC_EVENT = K8SClusterSyncEvent.class.getName();
  public final String EC2_INSTANCE_LIFECYCLE = Ec2Lifecycle.class.getName();
  public final String ECS_TASK_LIFECYCLE = EcsTaskLifecycle.class.getName();
  public final String ECS_CONTAINER_INSTANCE_INFO = EcsContainerInstanceInfo.class.getName();
  public final String ECS_CONTAINER_INSTANCE_LIFECYCLE = EcsContainerInstanceLifecycle.class.getName();
  public final String K8S_WATCH_EVENT = K8sWatchEvent.class.getName();
  public final String K8S_CONTAINER_STATE = ContainerStateProto.class.getName();
  public final String K8S_WORKLOAD_SPEC = K8sWorkloadSpec.class.getName();
}
