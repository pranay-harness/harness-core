package software.wings.service.impl.instance;

import static io.harness.exception.WingsException.EVERYBODY;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.AmiDeploymentType.SPOTINST;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.utils.Utils;

@Singleton
public class InstanceHandlerFactory {
  private ContainerInstanceHandler containerInstanceHandler;
  private AwsInstanceHandler awsInstanceHandler;
  private AwsAmiInstanceHandler awsAmiInstanceHandler;
  private AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler;
  private PcfInstanceHandler pcfInstanceHandler;
  private AzureInstanceHandler azureInstanceHandler;
  private SpotinstAmiInstanceHandler spotinstAmiInstanceHandler;
  private AwsLambdaInstanceHandler awsLambdaInstanceHandler;

  @Inject
  public InstanceHandlerFactory(ContainerInstanceHandler containerInstanceHandler,
      AwsInstanceHandler awsInstanceHandler, AwsAmiInstanceHandler awsAmiInstanceHandler,
      AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler, PcfInstanceHandler pcfInstanceHandler,
      AzureInstanceHandler azureInstanceHandler, SpotinstAmiInstanceHandler spotinstAmiInstanceHandler,
      AwsLambdaInstanceHandler awsLambdaInstanceHandler) {
    this.containerInstanceHandler = containerInstanceHandler;
    this.awsInstanceHandler = awsInstanceHandler;
    this.awsAmiInstanceHandler = awsAmiInstanceHandler;
    this.awsCodeDeployInstanceHandler = awsCodeDeployInstanceHandler;
    this.pcfInstanceHandler = pcfInstanceHandler;
    this.azureInstanceHandler = azureInstanceHandler;
    this.spotinstAmiInstanceHandler = spotinstAmiInstanceHandler;
    this.awsLambdaInstanceHandler = awsLambdaInstanceHandler;
  }

  private boolean isAmiSpotinstInfraMappingType(InfrastructureMapping infraMapping) {
    return infraMapping instanceof AwsAmiInfrastructureMapping
        && SPOTINST == ((AwsAmiInfrastructureMapping) infraMapping).getAmiDeploymentType();
  }

  public InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping) {
    InfrastructureMappingType infraMappingType =
        Utils.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());

    notNullCheck("Infra mapping type.", infraMappingType, EVERYBODY);

    switch (infraMappingType) {
      case AWS_SSH:
        return awsInstanceHandler;
      case AWS_AMI:
        if (isAmiSpotinstInfraMappingType(infraMapping)) {
          return spotinstAmiInstanceHandler;
        } else {
          return awsAmiInstanceHandler;
        }
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
        return awsLambdaInstanceHandler;
      case PHYSICAL_DATA_CENTER_SSH:
      case PHYSICAL_DATA_CENTER_WINRM:
        return null;
      default:
        throw new WingsException("No handler defined for infra mapping type: " + infraMappingType);
    }
  }
}