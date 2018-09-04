package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Singleton;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.PhysicalInfrastructureMappingWinRm.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

@Singleton
public class PhysicalInfraMappingWinRmYamlHandler
    extends PhysicalInfraMappingBaseYamlHandler<Yaml, PhysicalInfrastructureMappingWinRm> {
  @Override
  public Yaml toYaml(PhysicalInfrastructureMappingWinRm bean, String appId) {
    String winRmConnectionAttrsSettingId = bean.getWinRmConnectionAttributes();

    SettingAttribute settingAttribute = settingsService.get(winRmConnectionAttrsSettingId);
    notNullCheck(
        "WinRm connection attributes null for the given id: " + winRmConnectionAttrsSettingId, settingAttribute, USER);

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
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    PhysicalInfrastructureMappingWinRm current = new PhysicalInfrastructureMappingWinRm();
    toBean(changeContext, current, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    PhysicalInfrastructureMappingWinRm previous =
        (PhysicalInfrastructureMappingWinRm) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  public void toBean(ChangeContext<Yaml> changeContext, PhysicalInfrastructureMappingWinRm bean, String appId,
      String envId, String computeProviderId, String serviceId) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);

    String winRmConnAttrsName = yaml.getWinRmProfile();
    SettingAttribute winRmConnAttributes =
        settingsService.getSettingAttributeByName(changeContext.getChange().getAccountId(), winRmConnAttrsName);
    notNullCheck("HostConnectionAttrs is null for name:" + winRmConnAttributes, winRmConnAttrsName, USER);
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
