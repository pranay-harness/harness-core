package software.wings.graphql.schema.type.instance;

import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;

import io.fabric8.utils.Lists;
import java.util.List;

public enum QLInstanceType {
  PHYSICAL_HOST_INSTANCE(Lists.newArrayList(PhysicalHostInstanceInfo.class)),
  EC2_INSTANCE(Lists.newArrayList(Ec2InstanceInfo.class)),
  AUTOSCALING_GROUP_INSTANCE(Lists.newArrayList(AutoScalingGroupInstanceInfo.class)),
  CODE_DEPLOY_INSTANCE(Lists.newArrayList(CodeDeployInstanceInfo.class)),
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
