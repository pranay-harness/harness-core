package software.wings.service.impl.instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.exception.WingsException;

/**
 * Common methods needed by both instance and container instance.
 * This had to be created to avoid a cyclic dependency between InstanceHelper/ContainerInstanceHelper and
 * InstanceServiceImpl.
 * @author rktummala on 09/11/17
 */
public class InstanceUtil {
  private static final String WORKFLOW_PREFIX = "Workflow: ";
  private static final int WORKFLOW_PREFIX_LENGTH = 10;
  private static final Logger logger = LoggerFactory.getLogger(InstanceUtil.class);

  public void setInstanceType(Instance.Builder builder, String infraMappingType) {
    builder.withInstanceType(getInstanceType(infraMappingType));
  }

  public InstanceType getInstanceType(String infraMappingType) {
    InstanceType instanceType;
    if (InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.GCP_KUBERNETES.name().equals(infraMappingType)) {
      instanceType = InstanceType.KUBERNETES_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.AWS_ECS.name().equals(infraMappingType)) {
      instanceType = InstanceType.ECS_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(infraMappingType)) {
      instanceType = InstanceType.PHYSICAL_HOST_INSTANCE;
    } else if (InfrastructureMappingType.AWS_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AMI.name().equals(infraMappingType)) {
      instanceType = InstanceType.EC2_CLOUD_INSTANCE;
    } else {
      String msg = "Unsupported infraMapping type:" + infraMappingType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    return instanceType;
  }

  public String getWorkflowName(String workflowName) {
    if (workflowName == null) {
      return null;
    }

    if (workflowName.startsWith(WORKFLOW_PREFIX)) {
      return workflowName.substring(WORKFLOW_PREFIX_LENGTH);
    } else {
      return workflowName;
    }
  }

  public ContainerInstanceKey generateInstanceKeyForContainer(ContainerInfo containerInfo, InstanceType instanceType) {
    ContainerInstanceKey containerInstanceKey;

    if (instanceType == InstanceType.KUBERNETES_CONTAINER_INSTANCE) {
      KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.builder().containerId(kubernetesContainerInfo.getPodName()).build();

    } else if (instanceType == InstanceType.ECS_CONTAINER_INSTANCE) {
      EcsContainerInfo ecsContainerInfo = (EcsContainerInfo) containerInfo;
      containerInstanceKey = ContainerInstanceKey.builder().containerId(ecsContainerInfo.getTaskArn()).build();
    } else {
      String msg = "Unsupported container instance type:" + instanceType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    return containerInstanceKey;
  }
}
