package software.wings.service.impl.instance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.InfrastructureMappingType;
import software.wings.exception.WingsException;

/**
 * @author rktummala on 02/04/18
 */
@Singleton
public class InstanceHandlerFactory {
  @Inject private ContainerInstanceHandler containerInstanceHandler;
  @Inject private AwsInstanceHandler awsInstanceHandler;
  @Inject private AwsAmiInstanceHandler awsAmiInstanceHandler;
  @Inject private AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler;

  public InstanceHandler getInstanceHandler(InfrastructureMappingType infraMappingType) {
    switch (infraMappingType) {
      case AWS_SSH:
        return awsInstanceHandler;
      case AWS_AMI:
        return awsAmiInstanceHandler;
      case AWS_AWS_CODEDEPLOY:
        return awsCodeDeployInstanceHandler;
      case GCP_KUBERNETES:
      case AZURE_KUBERNETES:
      case DIRECT_KUBERNETES:
      case AWS_ECS:
        return containerInstanceHandler;
      case AWS_AWS_LAMBDA:
        throw new WingsException("No handler defined for Aws Lambda");
      case PHYSICAL_DATA_CENTER_SSH:
      case PHYSICAL_DATA_CENTER_WINRM:
        throw new WingsException("No handler defined for Physical host instance");
      default:
        throw new WingsException("No handler defined for infra mapping type: " + infraMappingType);
    }
  }
}
