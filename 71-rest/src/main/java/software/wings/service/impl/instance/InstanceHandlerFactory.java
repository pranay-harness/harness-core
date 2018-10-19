package software.wings.service.impl.instance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.InfrastructureMappingType;

/**
 * @author rktummala on 02/04/18
 */
@Singleton
public class InstanceHandlerFactory {
  private ContainerInstanceHandler containerInstanceHandler;
  private AwsInstanceHandler awsInstanceHandler;
  private AwsAmiInstanceHandler awsAmiInstanceHandler;
  private AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler;
  private PcfInstanceHandler pcfInstanceHandler;
  private AzureInstanceHandler azureInstanceHandler;

  @Inject
  public InstanceHandlerFactory(ContainerInstanceHandler containerInstanceHandler,
      AwsInstanceHandler awsInstanceHandler, AwsAmiInstanceHandler awsAmiInstanceHandler,
      AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler, PcfInstanceHandler pcfInstanceHandler,
      AzureInstanceHandler azureInstanceHandler) {
    this.containerInstanceHandler = containerInstanceHandler;
    this.awsInstanceHandler = awsInstanceHandler;
    this.awsAmiInstanceHandler = awsAmiInstanceHandler;
    this.awsCodeDeployInstanceHandler = awsCodeDeployInstanceHandler;
    this.pcfInstanceHandler = pcfInstanceHandler;
    this.azureInstanceHandler = azureInstanceHandler;
  }

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
      case AZURE_INFRA:
        return azureInstanceHandler;
      case PCF_PCF:
        return pcfInstanceHandler;
      case AWS_AWS_LAMBDA:
      case PHYSICAL_DATA_CENTER_SSH:
      case PHYSICAL_DATA_CENTER_WINRM:
        return null;
      default:
        throw new WingsException("No handler defined for infra mapping type: " + infraMappingType);
    }
  }
}
