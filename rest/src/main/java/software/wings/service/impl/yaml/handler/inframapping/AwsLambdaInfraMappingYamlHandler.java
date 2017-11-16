package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.AwsLambdaInfraStructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AwsLambdaInfraStructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class AwsLambdaInfraMappingYamlHandler
    extends InfraMappingYamlHandler<AwsLambdaInfraStructureMapping.Yaml, AwsLambdaInfraStructureMapping> {
  @Override
  public AwsLambdaInfraStructureMapping.Yaml toYaml(AwsLambdaInfraStructureMapping infraMapping, String appId) {
    return aYaml()
        .withType(InfrastructureMappingType.AWS_AWS_LAMBDA.name())
        .withRegion(infraMapping.getRegion())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withVpcId(infraMapping.getVpcId())
        .withSubnetIds(infraMapping.getSubnetIds())
        .withSecurityGroupIds(infraMapping.getSecurityGroupIds())
        .withRole(infraMapping.getRole())
        .build();
  }

  @Override
  public AwsLambdaInfraStructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    AwsLambdaInfraStructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + changeContext.getChange().getFilePath(), appId);
    String envId = yamlSyncHelper.getEnvironmentId(appId, changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + changeContext.getChange().getFilePath(), envId);
    String computeProviderId = getSettingId(appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck(
        "Couldn't retrieve compute provider from yaml:" + changeContext.getChange().getFilePath(), computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + changeContext.getChange().getFilePath(), serviceId);

    AwsLambdaInfraStructureMapping previous =
        (AwsLambdaInfraStructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);
    AwsLambdaInfraStructureMapping.Builder builder = previous.deepClone();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    return (AwsLambdaInfraStructureMapping) infraMappingService.update(builder.build());
  }

  private void setWithYamlValues(AwsLambdaInfraStructureMapping.Builder builder,
      AwsLambdaInfraStructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
      String serviceId) {
    builder.withAutoPopulate(false)
        .withComputeProviderSettingId(computeProviderId)
        .withComputeProviderName(infraMappingYaml.getComputeProviderName())
        .withComputeProviderType(infraMappingYaml.getComputeProviderType())
        .withEnvId(envId)
        .withServiceId(serviceId)
        .withInfraMappingType(infraMappingYaml.getInfraMappingType())
        .withDeploymentType(infraMappingYaml.getDeploymentType())
        .withName(infraMappingYaml.getName())
        .withAppId(appId);

    builder.withRegion(infraMappingYaml.getRegion())
        .withVpcId(infraMappingYaml.getVpcId())
        .withSubnetIds(infraMappingYaml.getSubnetIds())
        .withSecurityGroupIds(infraMappingYaml.getSecurityGroupIds())
        .withRole(infraMappingYaml.getRole())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsLambdaInfraStructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getRegion())
        || isEmpty(infraMappingYaml.getRole()) || isEmpty(infraMappingYaml.getSecurityGroupIds())
        || isEmpty(infraMappingYaml.getSubnetIds()) || isEmpty(infraMappingYaml.getVpcId()));
  }

  @Override
  public AwsLambdaInfraStructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    AwsLambdaInfraStructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + changeContext.getChange().getFilePath(), appId);
    String envId = yamlSyncHelper.getEnvironmentId(appId, changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + changeContext.getChange().getFilePath(), envId);
    String computeProviderId = getSettingId(appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck(
        "Couldn't retrieve compute provider from yaml:" + changeContext.getChange().getFilePath(), computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + changeContext.getChange().getFilePath(), serviceId);

    AwsLambdaInfraStructureMapping.Builder builder =
        AwsLambdaInfraStructureMapping.Builder.anAwsLambdaInfraStructureMapping();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    return (AwsLambdaInfraStructureMapping) infraMappingService.save(builder.build());
  }

  @Override
  public AwsLambdaInfraStructureMapping get(String accountId, String yamlFilePath) {
    return (AwsLambdaInfraStructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return AwsLambdaInfraStructureMapping.Yaml.class;
  }
}
