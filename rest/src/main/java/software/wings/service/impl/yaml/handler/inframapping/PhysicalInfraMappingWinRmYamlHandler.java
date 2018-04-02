package software.wings.service.impl.yaml.handler.inframapping;

import com.google.inject.Singleton;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.PhysicalInfrastructureMappingWinRm.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

@Singleton
public class PhysicalInfraMappingWinRmYamlHandler
    extends PhysicalInfraMappingBaseYamlHandler<Yaml, PhysicalInfrastructureMappingWinRm> {
  @Override
  public Yaml toYaml(PhysicalInfrastructureMappingWinRm bean, String appId) {
    String winRmConnectionAttrsSettingId = bean.getWinRmConnectionAttributes();

    SettingAttribute settingAttribute = settingsService.get(winRmConnectionAttrsSettingId);
    Validator.notNullCheck(
        "WinRm connection attributes null for the given id: " + winRmConnectionAttrsSettingId, settingAttribute);

    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM.name());
    yaml.setWinRmProfile(settingAttribute.getName());
    return yaml;
  }

  @Override
  public PhysicalInfrastructureMappingWinRm upsertFromYaml(
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

    PhysicalInfrastructureMappingWinRm current = new PhysicalInfrastructureMappingWinRm();
    toBean(changeContext, current, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    PhysicalInfrastructureMappingWinRm previous =
        (PhysicalInfrastructureMappingWinRm) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (PhysicalInfrastructureMappingWinRm) infraMappingService.update(current);
    } else {
      return (PhysicalInfrastructureMappingWinRm) infraMappingService.save(current);
    }
  }

  public void toBean(ChangeContext<Yaml> changeContext, PhysicalInfrastructureMappingWinRm bean, String appId,
      String envId, String computeProviderId, String serviceId) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId);

    String winRmConnAttrsName = yaml.getWinRmProfile();
    SettingAttribute winRmConnAttributes =
        settingsService.getSettingAttributeByName(changeContext.getChange().getAccountId(), winRmConnAttrsName);
    Validator.notNullCheck("HostConnectionAttrs is null for name:" + winRmConnAttributes, winRmConnAttrsName);
    bean.setWinRmConnectionAttributes(winRmConnAttributes.getUuid());
  }

  @Override
  public PhysicalInfrastructureMappingWinRm get(String accountId, String yamlFilePath) {
    return (PhysicalInfrastructureMappingWinRm) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
