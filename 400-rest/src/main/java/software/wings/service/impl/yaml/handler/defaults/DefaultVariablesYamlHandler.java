package software.wings.service.impl.yaml.handler.defaults;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.beans.Application;
import software.wings.beans.NameValuePairYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.defaults.DefaultsYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class DefaultVariablesYamlHandler extends BaseYamlHandler<DefaultsYaml, List<SettingAttribute>> {
  @Inject DefaultVariablesHelper defaultsHelper;
  @Inject YamlHelper yamlHelper;
  @Inject AppService appService;

  @Override
  public void delete(ChangeContext<DefaultsYaml> changeContext) {
    YamlType yamlType = changeContext.getYamlType();
    String accountId = changeContext.getChange().getAccountId();
    String appId;
    switch (yamlType) {
      case APPLICATION_DEFAULTS:
        Application application = yamlHelper.getApp(accountId, changeContext.getChange().getFilePath());
        if (application == null) {
          return;
        }
        appId = application.getUuid();
        break;
      case ACCOUNT_DEFAULTS:
        appId = GLOBAL_APP_ID;
        break;
      default:
        String msg = "UnSupported yamlType: " + yamlType;
        throw new WingsException(msg);
    }
    defaultsHelper.deleteDefaultVariables(accountId, appId);
  }

  @Override
  public DefaultsYaml toYaml(List<SettingAttribute> settingAttributeList, String appId) {
    List<NameValuePairYaml> nameValuePairYamlList = defaultsHelper.convertToNameValuePairYamlList(settingAttributeList);

    YamlType yamlType = GLOBAL_APP_ID.equals(appId) ? YamlType.ACCOUNT_DEFAULTS : YamlType.APPLICATION_DEFAULTS;

    return DefaultsYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .defaults(nameValuePairYamlList)
        .type(yamlType.name())
        .build();
  }

  @Override
  public List<SettingAttribute> upsertFromYaml(
      ChangeContext<DefaultsYaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    YamlType yamlType = changeContext.getYamlType();
    String accountId = changeContext.getChange().getAccountId();
    String appId;

    switch (yamlType) {
      case APPLICATION_DEFAULTS:
        appId = yamlHelper.getAppId(accountId, changeContext.getChange().getFilePath());
        break;
      case ACCOUNT_DEFAULTS:
        appId = GLOBAL_APP_ID;
        break;
      default:
        String msg = "UnSupported yamlType: " + yamlType;
        throw new WingsException(msg);
    }

    defaultsHelper.saveOrUpdateDefaults(
        changeContext.getYaml(), appId, accountId, changeContext.getChange().isSyncFromGit());
    return defaultsHelper.getCurrentDefaultVariables(appId, accountId);
  }

  @Override
  public Class getYamlClass() {
    return DefaultsYaml.class;
  }

  @Override
  public List<SettingAttribute> get(String accountId, String yamlFilePath) {
    String appName = yamlHelper.getAppName(yamlFilePath);
    if (appName == null) {
      return defaultsHelper.getCurrentDefaultVariables(GLOBAL_APP_ID, accountId);
    } else {
      Application app = appService.getAppByName(accountId, appName);
      return defaultsHelper.getCurrentDefaultVariables(app.getUuid(), accountId);
    }
  }
}
