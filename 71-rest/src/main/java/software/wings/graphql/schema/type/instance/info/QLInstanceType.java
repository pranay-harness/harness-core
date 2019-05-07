package software.wings.graphql.schema.type.instance.info;

import io.fabric8.utils.Lists;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;

import java.util.List;

public enum QLInstanceType {
  PHYSICAL_HOST_INSTANCE(Lists.newArrayList(PhysicalHostInstanceInfo.class)),
  EC2_CLOUD_INSTANCE(
      Lists.newArrayList(AutoScalingGroupInstanceInfo.class, Ec2InstanceInfo.class, CodeDeployInstanceInfo.class)),
  ECS_CONTAINER_INSTANCE(Lists.newArrayList(EcsContainerInfo.class)),
  KUBERNETES_CONTAINER_INSTANCE(Lists.newArrayList(KubernetesContainerInfo.class, K8sPodInfo.class)),
  PCF_INSTANCE(Lists.newArrayList(PcfInstanceInfo.class));

  List<Class<? extends InstanceInfo>> instanceInfos;

  QLInstanceType(List<Class<? extends InstanceInfo>> instanceInfos) {
    this.instanceInfos = instanceInfos;
  }

  public List<Class<? extends InstanceInfo>> getInstanceInfos() {
    return instanceInfos;
  }
}
