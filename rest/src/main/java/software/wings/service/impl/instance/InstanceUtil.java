package software.wings.service.impl.instance;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.utils.Validator;

/**
 * Common methods needed by both instance and container instance.
 * This had to be created to avoid a cyclic dependency between InstanceHelper and InstanceServiceImpl.
 * @author rktummala on 09/11/17
 */
public class InstanceUtil {
  public void setInstanceType(Instance.Builder builder, String infraMappingType) {
    InstanceType instanceType = null;
    if (InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.GCP_KUBERNETES.name().equals(infraMappingType)) {
      instanceType = InstanceType.KUBERNETES_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.AWS_ECS.name().equals(infraMappingType)) {
      instanceType = InstanceType.ECS_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(infraMappingType)) {
      instanceType = InstanceType.PHYSICAL_HOST_INSTANCE;
    } else if (InfrastructureMappingType.AWS_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name().equals(infraMappingType)) {
      instanceType = InstanceType.EC2_CLOUD_INSTANCE;
    }

    Validator.notNullCheck("InstanceType", instanceType);

    builder.withInstanceType(instanceType);
  }
}
