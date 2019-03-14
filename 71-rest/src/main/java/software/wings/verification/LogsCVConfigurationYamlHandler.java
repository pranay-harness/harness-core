package software.wings.verification;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration.ElkCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import java.util.List;

public class LogsCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<LogsCVConfigurationYaml, LogsCVConfiguration> {
  @Override
  public LogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    LogsCVConfigurationYaml yaml;
    switch (bean.getStateType()) {
      case SUMO:
        yaml = new LogsCVConfigurationYaml();
        break;
      case ELK:
        yaml = new ElkCVConfigurationYaml();
        break;
      default:
        throw new IllegalStateException("Invalid state " + bean.getStateType());
    }

    super.toYaml(yaml, bean);
    yaml.setQuery(bean.getQuery());
    yaml.setBaselineStartMinute(bean.getBaselineStartMinute());
    yaml.setBaselineEndMinute(bean.getBaselineEndMinute());
    yaml.setType(bean.getStateType().name());
    return yaml;
  }

  @Override
  public LogsCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    LogsCVConfiguration bean;
    switch (StateType.valueOf(changeContext.getYaml().getType())) {
      case SUMO:
        bean = new LogsCVConfiguration();
        break;
      case ELK:
        bean = new ElkCVConfiguration();
        break;
      default:
        throw new IllegalStateException("Invalid state " + changeContext.getYaml().getType());
    }

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
    return LogsCVConfigurationYaml.class;
  }

  @Override
  public LogsCVConfiguration get(String accountId, String yamlFilePath) {
    return (LogsCVConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void toBean(LogsCVConfiguration bean, ChangeContext<LogsCVConfigurationYaml> changeContext, String appId) {
    LogsCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setQuery(yaml.getQuery());
    bean.setBaselineStartMinute(yaml.getBaselineStartMinute());
    bean.setBaselineEndMinute(yaml.getBaselineEndMinute());
    bean.setStateType(StateType.valueOf(yaml.getType()));
  }
}
