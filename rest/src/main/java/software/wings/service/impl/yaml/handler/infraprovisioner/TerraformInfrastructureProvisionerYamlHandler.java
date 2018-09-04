package software.wings.service.impl.yaml.handler.infraprovisioner;

import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.intfc.SettingsService;

import java.util.List;

public class TerraformInfrastructureProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<Yaml, TerraformInfrastructureProvisioner> {
  @Inject SettingsService settingsService;

  protected String getSourceRepoSettingId(String appId, String sourceRepoSettingName) {
    SettingAttribute settingAttribute =
        settingsService.getSettingAttributeByName(SettingAttribute.GLOBAL_APP_ID, sourceRepoSettingName);
    notNullCheck("Invalid Source Repo Setting:" + sourceRepoSettingName, settingAttribute, USER);
    return settingAttribute.getUuid();
  }

  protected String getSourceRepoSettingName(String appId, String sourceRepoSettingId) {
    SettingAttribute settingAttribute = settingsService.get(SettingAttribute.GLOBAL_APP_ID, sourceRepoSettingId);
    notNullCheck("Invalid Source Repo Setting:" + sourceRepoSettingId, settingAttribute, USER);
    return settingAttribute.getName();
  }

  @Override
  public Yaml toYaml(TerraformInfrastructureProvisioner bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureProvisionerType.TERRAFORM.name());
    yaml.setPath(bean.getPath());
    yaml.setSourceRepoSettingName(getSourceRepoSettingName(appId, bean.getSourceRepoSettingId()));
    return yaml;
  }

  @Override
  public TerraformInfrastructureProvisioner upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    TerraformInfrastructureProvisioner current = TerraformInfrastructureProvisioner.builder().build();
    toBean(current, changeContext, appId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    TerraformInfrastructureProvisioner previous =
        (TerraformInfrastructureProvisioner) infrastructureProvisionerService.getByName(appId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      return (TerraformInfrastructureProvisioner) infrastructureProvisionerService.update(current);
    } else {
      return (TerraformInfrastructureProvisioner) infrastructureProvisionerService.save(current);
    }
  }

  private void toBean(TerraformInfrastructureProvisioner bean,
      ChangeContext<TerraformInfrastructureProvisioner.Yaml> changeContext, String appId) throws HarnessException {
    TerraformInfrastructureProvisioner.Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setPath(yaml.getPath());
    bean.setSourceRepoSettingId(getSourceRepoSettingId(appId, yaml.getSourceRepoSettingName()));
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public TerraformInfrastructureProvisioner get(String accountId, String yamlFilePath) {
    return (TerraformInfrastructureProvisioner) yamlHelper.getInfrastructureProvisioner(accountId, yamlFilePath);
  }
}
