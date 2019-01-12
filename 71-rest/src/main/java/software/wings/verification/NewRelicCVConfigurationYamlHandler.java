package software.wings.verification;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.sm.StateType;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration.NewRelicCVConfigurationYaml;

import java.util.ArrayList;
import java.util.List;

public class NewRelicCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<NewRelicCVConfigurationYaml, NewRelicCVServiceConfiguration> {
  @Override
  public NewRelicCVConfigurationYaml toYaml(NewRelicCVServiceConfiguration bean, String appId) {
    NewRelicCVConfigurationYaml yaml = NewRelicCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setApplicationId(bean.getApplicationId());
    yaml.setMetrics(bean.getMetrics());
    yaml.setType(StateType.NEW_RELIC.name());
    return yaml;
  }

  @Override
  public NewRelicCVServiceConfiguration upsertFromYaml(ChangeContext<NewRelicCVConfigurationYaml> changeContext,
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

    NewRelicCVServiceConfiguration bean = NewRelicCVServiceConfiguration.builder().build();
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
    return NewRelicCVConfigurationYaml.class;
  }

  @Override
  public NewRelicCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (NewRelicCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void toBean(NewRelicCVServiceConfiguration bean, ChangeContext<NewRelicCVConfigurationYaml> changeContext,
      String appId) throws HarnessException {
    NewRelicCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setMetrics(yaml.getMetrics() == null ? new ArrayList<>() : yaml.getMetrics());
    bean.setApplicationId(yaml.getApplicationId());
  }
}
