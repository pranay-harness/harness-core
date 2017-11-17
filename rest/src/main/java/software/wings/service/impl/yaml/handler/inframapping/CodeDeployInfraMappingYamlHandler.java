package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.CodeDeployInfrastructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder;
import software.wings.beans.CodeDeployInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class CodeDeployInfraMappingYamlHandler
    extends InfraMappingYamlHandler<CodeDeployInfrastructureMapping.Yaml, CodeDeployInfrastructureMapping> {
  @Override
  public CodeDeployInfrastructureMapping.Yaml toYaml(CodeDeployInfrastructureMapping infraMapping, String appId) {
    return aYaml()
        .withType(InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name())
        .withRegion(infraMapping.getRegion())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withApplicationName(infraMapping.getApplicationName())
        .withDeploymentGroup(infraMapping.getDeploymentGroup())
        .withDeploymentConfig(infraMapping.getDeploymentConfig())
        .build();
  }

  @Override
  public CodeDeployInfrastructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    CodeDeployInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

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

    CodeDeployInfrastructureMapping previous =
        (CodeDeployInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);
    CodeDeployInfrastructureMappingBuilder builder = previous.deepClone();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    return (CodeDeployInfrastructureMapping) infraMappingService.update(builder.build());
  }

  private void setWithYamlValues(CodeDeployInfrastructureMappingBuilder builder,
      CodeDeployInfrastructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
      String serviceId) {
    builder.withAutoPopulate(false)
        .withInfraMappingType(infraMappingYaml.getInfraMappingType())
        .withServiceTemplateId(getServiceTemplateId(appId, serviceId))
        .withComputeProviderSettingId(computeProviderId)
        .withComputeProviderName(infraMappingYaml.getComputeProviderName())
        .withComputeProviderType(infraMappingYaml.getComputeProviderType())
        .withEnvId(envId)
        .withServiceId(serviceId)
        .withDeploymentType(infraMappingYaml.getDeploymentType())
        .withName(infraMappingYaml.getName());

    builder.withRegion(infraMappingYaml.getRegion())
        .withApplicationName(infraMappingYaml.getApplicationName())
        .withDeploymentGroup(infraMappingYaml.getDeploymentGroup())
        .withDeploymentConfig(infraMappingYaml.getDeploymentConfig())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    CodeDeployInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getRegion())
        || isEmpty(infraMappingYaml.getApplicationName()) || isEmpty(infraMappingYaml.getDeploymentGroup())
        || isEmpty(infraMappingYaml.getDeploymentConfig()));
  }

  @Override
  public CodeDeployInfrastructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    CodeDeployInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

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

    CodeDeployInfrastructureMappingBuilder builder =
        CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    return (CodeDeployInfrastructureMapping) infraMappingService.save(builder.build());
  }

  @Override
  public CodeDeployInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (CodeDeployInfrastructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return CodeDeployInfrastructureMapping.Yaml.class;
  }
}
