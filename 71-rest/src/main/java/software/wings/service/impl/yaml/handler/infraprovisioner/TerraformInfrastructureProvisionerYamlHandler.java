package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Utils;

import java.util.List;

public class TerraformInfrastructureProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<Yaml, TerraformInfrastructureProvisioner> {
  @Inject SettingsService settingsService;
  @Inject AppService appService;

  protected String getSourceRepoSettingId(String appId, String sourceRepoSettingName) {
    Application application = appService.get(appId);

    SettingAttribute settingAttribute =
        settingsService.getSettingAttributeByName(application.getAccountId(), sourceRepoSettingName);
    notNullCheck("Invalid Source Repo Setting:" + sourceRepoSettingName, settingAttribute, USER);
    return settingAttribute.getUuid();
  }

  protected String getSourceRepoSettingName(String appId, String sourceRepoSettingId) {
    SettingAttribute settingAttribute = settingsService.get(GLOBAL_APP_ID, sourceRepoSettingId);
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
    yaml.setSourceRepoBranch(bean.getSourceRepoBranch());
    if (isNotEmpty(bean.getBackendConfigs())) {
      NameValuePairYamlHandler nameValuePairYamlHandler = getNameValuePairYamlHandler();
      List<NameValuePair.Yaml> nvpYamlList =
          bean.getBackendConfigs()
              .stream()
              .map(nameValuePair -> nameValuePairYamlHandler.toYaml(nameValuePair, bean.getAppId()))
              .collect(toList());

      yaml.setBackendConfigs(Utils.getSortedNameValuePairYamlList(nvpYamlList));
    }
    return yaml;
  }

  @Override
  public TerraformInfrastructureProvisioner upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
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
      current = (TerraformInfrastructureProvisioner) infrastructureProvisionerService.update(current);
    } else {
      current = (TerraformInfrastructureProvisioner) infrastructureProvisionerService.save(current);
    }

    changeContext.setEntity(current);
    return current;
  }

  private void toBean(TerraformInfrastructureProvisioner bean,
      ChangeContext<TerraformInfrastructureProvisioner.Yaml> changeContext, String appId) {
    TerraformInfrastructureProvisioner.Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setPath(yaml.getPath());
    bean.setSourceRepoSettingId(getSourceRepoSettingId(appId, yaml.getSourceRepoSettingName()));
    bean.setSourceRepoBranch(yaml.getSourceRepoBranch());

    if (isNotEmpty(yaml.getBackendConfigs())) {
      List<NameValuePair> nameValuePairList = yaml.getBackendConfigs()
                                                  .stream()
                                                  .map(nvpYaml
                                                      -> NameValuePair.builder()
                                                             .name(nvpYaml.getName())
                                                             .value(nvpYaml.getValue())
                                                             .valueType(nvpYaml.getValueType())
                                                             .build())
                                                  .collect(toList());

      bean.setBackendConfigs(nameValuePairList);
    }
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
