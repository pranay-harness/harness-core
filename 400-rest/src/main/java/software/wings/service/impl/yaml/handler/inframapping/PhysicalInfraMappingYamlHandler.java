package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMappingYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class PhysicalInfraMappingYamlHandler
    extends PhysicalInfraMappingBaseYamlHandler<PhysicalInfrastructureMappingYaml, PhysicalInfrastructureMapping> {
  @Override
  public PhysicalInfrastructureMappingYaml toYaml(PhysicalInfrastructureMapping bean, String appId) {
    String hostConnectionAttrsSettingId = bean.getHostConnectionAttrs();

    SettingAttribute settingAttribute = settingsService.get(hostConnectionAttrsSettingId);
    notNullCheck(
        "Host connection attributes null for the given id: " + hostConnectionAttrsSettingId, settingAttribute, USER);

    PhysicalInfrastructureMappingYaml yaml = PhysicalInfrastructureMappingYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name());
    yaml.setConnection(settingAttribute.getName());
    return yaml;
  }

  @Override
  public PhysicalInfrastructureMapping upsertFromYaml(
      ChangeContext<PhysicalInfrastructureMappingYaml> changeContext, List<ChangeContext> changeSetContext) {
    PhysicalInfrastructureMappingYaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    PhysicalInfrastructureMapping current = new PhysicalInfrastructureMapping();
    toBean(changeContext, current, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    PhysicalInfrastructureMapping previous =
        (PhysicalInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  @Override
  protected void toBean(ChangeContext<PhysicalInfrastructureMappingYaml> changeContext,
      PhysicalInfrastructureMapping bean, String appId, String envId, String computeProviderId, String serviceId) {
    PhysicalInfrastructureMappingYaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);

    String hostConnAttrsName = yaml.getConnection();
    SettingAttribute hostConnAttributes =
        settingsService.getSettingAttributeByName(changeContext.getChange().getAccountId(), hostConnAttrsName);
    notNullCheck("HostConnectionAttrs is null for name:" + hostConnAttributes, hostConnAttrsName, USER);
    bean.setHostConnectionAttrs(hostConnAttributes.getUuid());
  }

  @Override
  public PhysicalInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (PhysicalInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return PhysicalInfrastructureMappingYaml.class;
  }
}
