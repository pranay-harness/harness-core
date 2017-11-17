package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.DirectKubernetesInfrastructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class DirectKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlHandler<DirectKubernetesInfrastructureMapping.Yaml, DirectKubernetesInfrastructureMapping> {
  @Override
  public DirectKubernetesInfrastructureMapping.Yaml toYaml(
      DirectKubernetesInfrastructureMapping infraMapping, String appId) {
    return aYaml()
        .withType(InfrastructureMappingType.DIRECT_KUBERNETES.name())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withMasterUrl(infraMapping.getMasterUrl())
        .withUsername(infraMapping.getUsername())
        .withPassword(infraMapping.getPassword())
        .withNamespace(infraMapping.getNamespace())
        .build();
  }

  @Override
  public DirectKubernetesInfrastructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    DirectKubernetesInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

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

    DirectKubernetesInfrastructureMapping previous =
        (DirectKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);
    DirectKubernetesInfrastructureMapping.Builder builder = previous.deepClone();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    return (DirectKubernetesInfrastructureMapping) infraMappingService.update(builder.build());
  }

  private void setWithYamlValues(DirectKubernetesInfrastructureMapping.Builder builder,
      DirectKubernetesInfrastructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
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

    builder.withMasterUrl(infraMappingYaml.getMasterUrl())
        .withUsername(infraMappingYaml.getUsername())
        .withPassword(infraMappingYaml.getPassword())
        .withNamespace(infraMappingYaml.getNamespace())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    DirectKubernetesInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getMasterUrl())
        || isEmpty(infraMappingYaml.getUsername()) || isEmpty(infraMappingYaml.getPassword().toString())
        || isEmpty(infraMappingYaml.getNamespace()));
  }

  @Override
  public DirectKubernetesInfrastructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    DirectKubernetesInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

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

    DirectKubernetesInfrastructureMapping.Builder builder =
        DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping();
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    return (DirectKubernetesInfrastructureMapping) infraMappingService.save(builder.build());
  }

  @Override
  public DirectKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (DirectKubernetesInfrastructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return DirectKubernetesInfrastructureMapping.Yaml.class;
  }
}
