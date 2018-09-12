package software.wings.service.impl.instance;

import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;

/**
 * Common methods needed by both instance and container instance.
 * This had to be created to avoid a cyclic dependency between InstanceHelper/ContainerInstanceHelper and
 * InstanceServiceImpl.
 * @author rktummala on 09/11/17
 */
@Singleton
public class InstanceUtil {
  private static final String WORKFLOW_PREFIX = "Workflow: ";
  private static final int WORKFLOW_PREFIX_LENGTH = 10;
  private static final Logger logger = LoggerFactory.getLogger(InstanceUtil.class);

  public void setInstanceType(InstanceBuilder builder, String infraMappingType) {
    builder.instanceType(getInstanceType(infraMappingType));
  }

  public InstanceType getInstanceType(String infraMappingType) {
    InstanceType instanceType;
    if (InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.AZURE_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.GCP_KUBERNETES.name().equals(infraMappingType)) {
      instanceType = InstanceType.KUBERNETES_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.AWS_ECS.name().equals(infraMappingType)) {
      instanceType = InstanceType.ECS_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM.name().equals(infraMappingType)) {
      instanceType = InstanceType.PHYSICAL_HOST_INSTANCE;
    } else if (InfrastructureMappingType.AWS_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AMI.name().equals(infraMappingType)) {
      instanceType = InstanceType.EC2_CLOUD_INSTANCE;
    } else if (InfrastructureMappingType.PCF_PCF.name().equals(infraMappingType)) {
      instanceType = InstanceType.PCF_INSTANCE;
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
}
