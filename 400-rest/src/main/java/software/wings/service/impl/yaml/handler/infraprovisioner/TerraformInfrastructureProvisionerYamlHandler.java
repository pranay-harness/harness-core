package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import static java.util.stream.Collectors.toList;

import io.harness.beans.SecretManagerConfig;
import io.harness.exception.InvalidRequestException;

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
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import java.util.List;

public class TerraformInfrastructureProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<Yaml, TerraformInfrastructureProvisioner> {
  @Inject SettingsService settingsService;
  @Inject AppService appService;
  @Inject SecretManager secretManager;

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
    yaml.setCommitId(bean.getCommitId());
    yaml.setRepoName(bean.getRepoName());
    if (isNotEmpty(bean.getBackendConfigs())) {
      yaml.setBackendConfigs(getSortedNameValuePairYamlList(bean.getBackendConfigs(), bean.getAppId()));
    }
    if (isNotEmpty(bean.getEnvironmentVariables())) {
      yaml.setEnvironmentVariables(getSortedNameValuePairYamlList(bean.getEnvironmentVariables(), bean.getAppId()));
    }
    if (isNotEmpty(bean.getKmsId())) {
      SecretManagerConfig secretManagerConfig = secretManager.getSecretManager(bean.getAccountId(), bean.getKmsId());
      yaml.setSecretMangerName(secretManagerConfig.getName());
    }
    yaml.setSkipRefreshBeforeApplyingPlan(bean.isSkipRefreshBeforeApplyingPlan());
    return yaml;
  }

  private List<software.wings.beans.NameValuePair.Yaml> getSortedNameValuePairYamlList(
      List<NameValuePair> nameValuePairList, String appId) {
    NameValuePairYamlHandler nameValuePairYamlHandler = getNameValuePairYamlHandler();
    List<NameValuePair.Yaml> nvpYamlList =
        nameValuePairList.stream()
            .map(nameValuePair -> nameValuePairYamlHandler.toYaml(nameValuePair, appId))
            .collect(toList());
    return Utils.getSortedNameValuePairYamlList(nvpYamlList);
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
    validateBranchCommitId(yaml.getSourceRepoBranch(), yaml.getCommitId());
    bean.setSourceRepoBranch(yaml.getSourceRepoBranch());
    bean.setCommitId(yaml.getCommitId());
    bean.setRepoName(yaml.getRepoName());
    bean.setSkipRefreshBeforeApplyingPlan(yaml.isSkipRefreshBeforeApplyingPlan());

    if (isNotEmpty(yaml.getBackendConfigs())) {
      bean.setBackendConfigs(getNameValuePairList(yaml.getBackendConfigs()));
    }
    if (isNotEmpty(yaml.getEnvironmentVariables())) {
      bean.setEnvironmentVariables(getNameValuePairList(yaml.getEnvironmentVariables()));
    }
    if (isNotEmpty(yaml.getSecretMangerName())) {
      SecretManagerConfig secretManagerConfig =
          secretManager.getSecretManagerByName(bean.getAccountId(), yaml.getSecretMangerName());
      bean.setKmsId(secretManagerConfig.getUuid());
    }
  }

  private List<NameValuePair> getNameValuePairList(List<NameValuePair.Yaml> nameValuePairList) {
    return nameValuePairList.stream()
        .map(nvpYaml
            -> NameValuePair.builder()
                   .name(nvpYaml.getName())
                   .value(nvpYaml.getValue())
                   .valueType(nvpYaml.getValueType())
                   .build())
        .collect(toList());
  }

  private void validateBranchCommitId(String sourceRepoBranch, String commitId) {
    if (isEmpty(sourceRepoBranch) && isEmpty(commitId)) {
      throw new InvalidRequestException("Either sourceRepoBranch or commitId should be specified", USER);
    }
    if (isNotEmpty(sourceRepoBranch) && isNotEmpty(commitId)) {
      throw new InvalidRequestException("Cannot specify both sourceRepoBranch and commitId", USER);
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
