/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.exception.WingsException.EVERYBODY;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.AmiDeploymentType.SPOTINST;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.UnexpectedException;

import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.utils.Utils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

@Singleton
@TargetModule(HarnessModule._441_CG_INSTANCE_SYNC)
public class InstanceHandlerFactory implements InstanceHandlerFactoryService {
  private ContainerInstanceHandler containerInstanceHandler;
  private AwsInstanceHandler awsInstanceHandler;
  private AwsAmiInstanceHandler awsAmiInstanceHandler;
  private AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler;
  private PcfInstanceHandler pcfInstanceHandler;
  private AzureInstanceHandler azureInstanceHandler;
  private SpotinstAmiInstanceHandler spotinstAmiInstanceHandler;
  private AwsLambdaInstanceHandler awsLambdaInstanceHandler;
  private CustomDeploymentInstanceHandler customDeploymentInstanceHandler;
  private AzureVMSSInstanceHandler azureVMSSInstanceHandler;
  private AzureWebAppInstanceHandler azureWebAppInstanceHandler;

  @Inject
  public InstanceHandlerFactory(ContainerInstanceHandler containerInstanceHandler,
      AwsInstanceHandler awsInstanceHandler, AwsAmiInstanceHandler awsAmiInstanceHandler,
      AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler, PcfInstanceHandler pcfInstanceHandler,
      AzureInstanceHandler azureInstanceHandler, SpotinstAmiInstanceHandler spotinstAmiInstanceHandler,
      AwsLambdaInstanceHandler awsLambdaInstanceHandler,
      CustomDeploymentInstanceHandler customDeploymentInstanceHandler,
      AzureVMSSInstanceHandler azureVMSSInstanceHandler, AzureWebAppInstanceHandler azureWebAppInstanceHandler) {
    this.containerInstanceHandler = containerInstanceHandler;
    this.awsInstanceHandler = awsInstanceHandler;
    this.awsAmiInstanceHandler = awsAmiInstanceHandler;
    this.awsCodeDeployInstanceHandler = awsCodeDeployInstanceHandler;
    this.pcfInstanceHandler = pcfInstanceHandler;
    this.azureInstanceHandler = azureInstanceHandler;
    this.spotinstAmiInstanceHandler = spotinstAmiInstanceHandler;
    this.awsLambdaInstanceHandler = awsLambdaInstanceHandler;
    this.customDeploymentInstanceHandler = customDeploymentInstanceHandler;
    this.azureVMSSInstanceHandler = azureVMSSInstanceHandler;
    this.azureWebAppInstanceHandler = azureWebAppInstanceHandler;
  }

  private boolean isAmiSpotinstInfraMappingType(InfrastructureMapping infraMapping) {
    return infraMapping instanceof AwsAmiInfrastructureMapping
        && SPOTINST == ((AwsAmiInfrastructureMapping) infraMapping).getAmiDeploymentType();
  }

  @Override
  public InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping) {
    InfrastructureMappingType infraMappingType =
        Utils.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());

    notNullCheck("Infra mapping type.", infraMappingType, EVERYBODY);

    switch (infraMappingType) {
      case AZURE_VMSS:
        return azureVMSSInstanceHandler;
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
      case AZURE_WEBAPP:
        return azureWebAppInstanceHandler;
      case PCF_PCF:
        return pcfInstanceHandler;
      case AWS_AWS_LAMBDA:
        return awsLambdaInstanceHandler;
      case PHYSICAL_DATA_CENTER_SSH:
      case PHYSICAL_DATA_CENTER_WINRM:
        return null;
      case CUSTOM:
        return customDeploymentInstanceHandler;
      default:
        throw new UnexpectedException("No handler defined for infra mapping type: " + infraMappingType);
    }
  }

  @Override
  public Set<InstanceHandler> getAllInstanceHandlers() {
    return Sets.newHashSet(containerInstanceHandler, awsInstanceHandler, awsAmiInstanceHandler,
        awsCodeDeployInstanceHandler, pcfInstanceHandler, azureInstanceHandler, spotinstAmiInstanceHandler,
        awsLambdaInstanceHandler, customDeploymentInstanceHandler, azureVMSSInstanceHandler,
        azureWebAppInstanceHandler);
  }
}
