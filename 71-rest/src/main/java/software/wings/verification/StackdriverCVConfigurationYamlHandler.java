package software.wings.verification;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import io.harness.exception.WingsException;
import software.wings.beans.yaml.ChangeContext;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;
import software.wings.verification.log.StackdriverCVConfiguration;
import software.wings.verification.log.StackdriverCVConfiguration.StackdriverCVConfigurationYaml;

import java.util.List;

/**
 * Created by Pranjal on 06/04/2019
 */
public class StackdriverCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public StackdriverCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    final StackdriverCVConfigurationYaml yaml = (StackdriverCVConfigurationYaml) super.toYaml(bean, appId);
    if (!(bean instanceof StackdriverCVConfiguration)) {
      throw new WingsException("Unexpected type of cluster configuration");
    }

    StackdriverCVConfiguration stackdriverCVConfiguration = (StackdriverCVConfiguration) bean;
    yaml.setQuery(stackdriverCVConfiguration.getQuery());
    yaml.setLogsConfiguration(stackdriverCVConfiguration.isLogsConfiguration());
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

    final StackdriverCVConfiguration bean = StackdriverCVConfiguration.builder().build();

    super.toBean(bean, changeContext, appId);

    StackdriverCVConfigurationYaml yaml = (StackdriverCVConfigurationYaml) changeContext.getYaml();
    bean.setQuery(yaml.getQuery());
    bean.setLogsConfiguration(yaml.isLogsConfiguration());

    saveToDatabase(bean, previous, appId);

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return StackdriverCVConfigurationYamlHandler.class;
  }
}
