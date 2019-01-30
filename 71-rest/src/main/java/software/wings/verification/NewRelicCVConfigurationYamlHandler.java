package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration.NewRelicCVConfigurationYaml;

import java.util.ArrayList;
import java.util.List;

public class NewRelicCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<NewRelicCVConfigurationYaml, NewRelicCVServiceConfiguration> {
  @Inject NewRelicService newRelicService;

  @Override
  public NewRelicCVConfigurationYaml toYaml(NewRelicCVServiceConfiguration bean, String appId) {
    NewRelicCVConfigurationYaml yaml = NewRelicCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    List<NewRelicApplication> newRelicApplications =
        newRelicService.getApplications(bean.getConnectorId(), StateType.NEW_RELIC);
    for (NewRelicApplication newRelicApplication : newRelicApplications) {
      if (String.valueOf(newRelicApplication.getId()).equals(bean.getApplicationId())) {
        yaml.setNewRelicApplicationName(newRelicApplication.getName());
        break;
      }
    }
    if (isEmpty(yaml.getNewRelicApplicationName())) {
      throw new WingsException("Invalid NewRelic ApplicationID when converting to YAML: " + bean.getApplicationId());
    }
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

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

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
    SettingAttribute connector = getConnector(yaml);
    List<NewRelicApplication> newRelicApplications =
        newRelicService.getApplications(connector.getUuid(), StateType.NEW_RELIC);
    for (NewRelicApplication newRelicApplication : newRelicApplications) {
      if (newRelicApplication.getName().equals(yaml.getNewRelicApplicationName())) {
        bean.setApplicationId(String.valueOf(newRelicApplication.getId()));
        break;
      }
    }
    if (isEmpty(bean.getApplicationId())) {
      throw new WingsException(
          "Invalid NewRelic Application name when saving YAML: " + yaml.getNewRelicApplicationName());
    }
    bean.setStateType(StateType.NEW_RELIC);
  }
}
