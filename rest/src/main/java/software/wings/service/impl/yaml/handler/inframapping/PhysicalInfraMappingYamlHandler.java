package software.wings.service.impl.yaml.handler.inframapping;

import com.google.inject.Singleton;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

@Singleton
public class PhysicalInfraMappingYamlHandler
    extends PhysicalInfraMappingBaseYamlHandler<Yaml, PhysicalInfrastructureMapping> {
  @Override
  public Yaml toYaml(PhysicalInfrastructureMapping bean, String appId) {
    String hostConnectionAttrsSettingId = bean.getHostConnectionAttrs();

    SettingAttribute settingAttribute = settingsService.get(hostConnectionAttrsSettingId);
    Validator.notNullCheck(
        "Host connection attributes null for the given id: " + hostConnectionAttrsSettingId, settingAttribute);

    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name());
    yaml.setConnection(settingAttribute.getName());
    return yaml;
  }

  @Override
  public PhysicalInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId);

    PhysicalInfrastructureMapping current = new PhysicalInfrastructureMapping();
    toBean(changeContext, current, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    PhysicalInfrastructureMapping previous =
        (PhysicalInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (PhysicalInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (PhysicalInfrastructureMapping) infraMappingService.save(current);
    }
  }

  protected void toBean(ChangeContext<Yaml> changeContext, PhysicalInfrastructureMapping bean, String appId,
      String envId, String computeProviderId, String serviceId) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId);

    String hostConnAttrsName = yaml.getConnection();
    SettingAttribute hostConnAttributes =
        settingsService.getSettingAttributeByName(changeContext.getChange().getAccountId(), hostConnAttrsName);
    Validator.notNullCheck("HostConnectionAttrs is null for name:" + hostConnAttributes, hostConnAttrsName);
    bean.setHostConnectionAttrs(hostConnAttributes.getUuid());
  }

  @Override
  public PhysicalInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (PhysicalInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
