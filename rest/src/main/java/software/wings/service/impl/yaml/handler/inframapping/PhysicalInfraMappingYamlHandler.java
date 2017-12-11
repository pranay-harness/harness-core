package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.PhysicalInfrastructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class PhysicalInfraMappingYamlHandler
    extends InfraMappingYamlHandler<PhysicalInfrastructureMapping.Yaml, PhysicalInfrastructureMapping> {
  @Override
  public PhysicalInfrastructureMapping.Yaml toYaml(PhysicalInfrastructureMapping infraMapping, String appId) {
    return aYaml()
        .withType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withComputeProviderType(infraMapping.getComputeProviderType())
        .withConnection(infraMapping.getHostConnectionAttrs())
        .withHostNames(infraMapping.getHostNames())
        .withLoadBalancer(infraMapping.getLoadBalancerName())
        .build();
  }

  @Override
  public PhysicalInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    PhysicalInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

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

    PhysicalInfrastructureMapping.Builder builder =
        PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping().withAccountId(
            changeContext.getChange().getAccountId());
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    PhysicalInfrastructureMapping current = builder.build();

    PhysicalInfrastructureMapping previous =
        (PhysicalInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (PhysicalInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (PhysicalInfrastructureMapping) infraMappingService.save(current);
    }
  }

  @Override
  public PhysicalInfrastructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public PhysicalInfrastructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  private void setWithYamlValues(PhysicalInfrastructureMapping.Builder builder,
      PhysicalInfrastructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
      String serviceId) {
    // common stuff for all infra mapping
    builder.withAutoPopulate(false)
        .withInfraMappingType(infraMappingYaml.getInfraMappingType())
        .withName(infraMappingYaml.getName())
        .withServiceTemplateId(getServiceTemplateId(appId, serviceId))
        .withComputeProviderSettingId(computeProviderId)
        .withComputeProviderName(infraMappingYaml.getComputeProviderName())
        .withComputeProviderType(infraMappingYaml.getComputeProviderType())
        .withEnvId(envId)
        .withServiceId(serviceId)
        .withServiceTemplateId(getServiceTemplateId(appId, serviceId))
        .withDeploymentType(infraMappingYaml.getDeploymentType())
        .withName(infraMappingYaml.getName())
        .withAppId(appId);

    if (!Misc.isNullOrEmpty(infraMappingYaml.getLoadBalancer())) {
      builder.withLoadBalancerId(getSettingId(appId, infraMappingYaml.getLoadBalancer()));
    }
    builder.withHostConnectionAttrs(infraMappingYaml.getConnection())
        .withHostNames(infraMappingYaml.getHostNames())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    PhysicalInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getConnection())
        || isEmpty(infraMappingYaml.getHostNames()));
  }

  @Override
  public PhysicalInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (PhysicalInfrastructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return PhysicalInfrastructureMapping.Yaml.class;
  }
}
