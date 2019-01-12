package software.wings.verification;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration.AppDynamicsCVConfigurationYaml;

import java.util.List;

public class AppDynamicsCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<AppDynamicsCVConfigurationYaml, AppDynamicsCVServiceConfiguration> {
  @Override
  public AppDynamicsCVConfigurationYaml toYaml(AppDynamicsCVServiceConfiguration bean, String appId) {
    AppDynamicsCVConfigurationYaml yaml = AppDynamicsCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setAppDynamicsApplicationId(bean.getAppDynamicsApplicationId());
    yaml.setTierId(bean.getTierId());
    yaml.setType(StateType.APP_DYNAMICS.name());
    return yaml;
  }

  @Override
  public AppDynamicsCVServiceConfiguration upsertFromYaml(ChangeContext<AppDynamicsCVConfigurationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String serviceId = changeContext.getYaml().getServiceId();
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    notNullCheck("EnvironmentId null in yaml for CVConfiguration", envId, USER);
    notNullCheck("ServiceId null in yaml for CVConfiguration", serviceId, USER);

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    AppDynamicsCVServiceConfiguration bean = AppDynamicsCVServiceConfiguration.builder().build();
    toBean(bean, changeContext, appId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      cvConfigurationService.saveCofiguration(bean);
    }

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return AppDynamicsCVConfigurationYaml.class;
  }

  @Override
  public AppDynamicsCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (AppDynamicsCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void toBean(AppDynamicsCVServiceConfiguration bean,
      ChangeContext<AppDynamicsCVConfigurationYaml> changeContext, String appId) throws HarnessException {
    AppDynamicsCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setAppDynamicsApplicationId(yaml.getAppDynamicsApplicationId());
    bean.setTierId(yaml.getTierId());
  }
}
