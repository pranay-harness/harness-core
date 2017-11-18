package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.EcsInfrastructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class EcsInfraMappingYamlHandler
    extends InfraMappingYamlHandler<EcsInfrastructureMapping.Yaml, EcsInfrastructureMapping> {
  @Override
  public EcsInfrastructureMapping.Yaml toYaml(EcsInfrastructureMapping infraMapping, String appId) {
    return aYaml()
        .withType(InfrastructureMappingType.AWS_ECS.name())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withRegion(infraMapping.getRegion())
        .build();
  }

  @Override
  public EcsInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    EcsInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + changeContext.getChange().getFilePath(), appId);
    String envId = yamlSyncHelper.getEnvironmentId(appId, changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + changeContext.getChange().getFilePath(), envId);
    String computeProviderId = getSettingId(appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck(
        "Couldn't retrieve compute provider from yaml:" + changeContext.getChange().getFilePath(), computeProviderId);
    String serviceId = yamlSyncHelper.getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + changeContext.getChange().getFilePath(), serviceId);

    EcsInfrastructureMapping.Builder builder = EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    EcsInfrastructureMapping current = builder.build();
    EcsInfrastructureMapping previous =
        (EcsInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (EcsInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (EcsInfrastructureMapping) infraMappingService.save(current);
    }
  }

  @Override
  public EcsInfrastructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  private void setWithYamlValues(EcsInfrastructureMapping.Builder builder,
      EcsInfrastructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
      String serviceId) {
    // common stuff for all infra mapping
    builder.withAutoPopulate(false)
        .withInfraMappingType(infraMappingYaml.getInfraMappingType())
        .withServiceTemplateId(getServiceTemplateId(appId, serviceId))
        .withComputeProviderSettingId(computeProviderId)
        .withComputeProviderName(infraMappingYaml.getComputeProviderName())
        .withComputeProviderType(infraMappingYaml.getComputeProviderType())
        .withEnvId(envId)
        .withServiceId(serviceId)
        .withDeploymentType(infraMappingYaml.getDeploymentType())
        .withName(infraMappingYaml.getName())
        .withAppId(appId);

    builder.withRegion(infraMappingYaml.getRegion()).build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    EcsInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getRegion()));
  }

  @Override
  public EcsInfrastructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public EcsInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (EcsInfrastructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return EcsInfrastructureMapping.Yaml.class;
  }
}
