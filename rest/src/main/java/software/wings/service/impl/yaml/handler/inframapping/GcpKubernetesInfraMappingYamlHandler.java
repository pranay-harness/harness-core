package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.GcpKubernetesInfrastructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class GcpKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlHandler<GcpKubernetesInfrastructureMapping.Yaml, GcpKubernetesInfrastructureMapping> {
  @Override
  public GcpKubernetesInfrastructureMapping.Yaml toYaml(GcpKubernetesInfrastructureMapping infraMapping, String appId) {
    return aYaml()
        .withType(InfrastructureMappingType.GCP_KUBERNETES.name())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withNamespace(infraMapping.getNamespace())
        .build();
  }

  @Override
  public GcpKubernetesInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    GcpKubernetesInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

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

    GcpKubernetesInfrastructureMapping.Builder builder =
        GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping().withAccountId(
            changeContext.getChange().getAccountId());
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    GcpKubernetesInfrastructureMapping current = builder.build();
    GcpKubernetesInfrastructureMapping previous =
        (GcpKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (GcpKubernetesInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (GcpKubernetesInfrastructureMapping) infraMappingService.save(current);
    }
  }

  @Override
  public GcpKubernetesInfrastructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  private void setWithYamlValues(GcpKubernetesInfrastructureMapping.Builder builder,
      GcpKubernetesInfrastructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
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

    builder.withNamespace(infraMappingYaml.getNamespace()).build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    GcpKubernetesInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getNamespace()));
  }

  @Override
  public GcpKubernetesInfrastructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public GcpKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (GcpKubernetesInfrastructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return GcpKubernetesInfrastructureMapping.Yaml.class;
  }
}
