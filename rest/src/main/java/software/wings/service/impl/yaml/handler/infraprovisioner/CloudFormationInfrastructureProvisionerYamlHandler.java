package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.CloudFormationInfrastructureProvisioner.Yaml;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.intfc.SettingsService;

import java.util.List;

public class CloudFormationInfrastructureProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<Yaml, CloudFormationInfrastructureProvisioner> {
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
  public Yaml toYaml(CloudFormationInfrastructureProvisioner bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureProvisionerType.CLOUD_FORMATION.name());
    yaml.setSourceType(bean.getSourceType());
    yaml.setTemplateBody(bean.getTemplateBody());
    yaml.setTemplateFilePath(bean.getTemplateFilePath());
    return yaml;
  }

  @Override
  public CloudFormationInfrastructureProvisioner upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    CloudFormationInfrastructureProvisioner current = CloudFormationInfrastructureProvisioner.builder().build();
    toBean(current, changeContext, appId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    CloudFormationInfrastructureProvisioner previous =
        (CloudFormationInfrastructureProvisioner) infrastructureProvisionerService.getByName(appId, name);

    current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (CloudFormationInfrastructureProvisioner) infrastructureProvisionerService.update(current);
    } else {
      return (CloudFormationInfrastructureProvisioner) infrastructureProvisionerService.save(current);
    }
  }

  private void toBean(CloudFormationInfrastructureProvisioner bean, ChangeContext<Yaml> changeContext, String appId)
      throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setTemplateFilePath(yaml.getTemplateFilePath());
    bean.setTemplateBody(yaml.getTemplateBody());
    bean.setSourceType(yaml.getSourceType());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public CloudFormationInfrastructureProvisioner get(String accountId, String yamlFilePath) {
    return (CloudFormationInfrastructureProvisioner) yamlHelper.getInfrastructureProvisioner(accountId, yamlFilePath);
  }
}
