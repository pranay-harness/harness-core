package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import io.harness.exception.WingsException;
import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DatadogCvConfigurationYamlHandler
    extends CVConfigurationYamlHandler<DatadogCVConfigurationYaml, DatadogCVServiceConfiguration> {
  @Override
  public DatadogCVConfigurationYaml toYaml(DatadogCVServiceConfiguration bean, String appId) {
    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDatadogServiceName(bean.getDatadogServiceName());
    yaml.setMetrics(bean.getMetrics());
    yaml.setApplicationFilter(bean.getApplicationFilter());
    yaml.setType(StateType.DATA_DOG.name());
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return DatadogCVConfigurationYaml.class;
  }

  @Override
  public DatadogCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (DatadogCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  @Override
  public DatadogCVServiceConfiguration upsertFromYaml(
      ChangeContext<DatadogCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    DatadogCVServiceConfiguration bean = DatadogCVServiceConfiguration.builder().build();
    toBean(bean, changeContext, appId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      cvConfigurationService.saveCofiguration(bean);
    }

    return bean;
  }

  private void toBean(
      DatadogCVServiceConfiguration bean, ChangeContext<DatadogCVConfigurationYaml> changeContext, String appId) {
    DatadogCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    if (isEmpty(yaml.getApplicationFilter()) || isEmpty(yaml.getMetrics())) {
      throw new WingsException("Invalid Datadog yaml. Please set valid application filter and metrics");
    }
    // validate if the metrics in yaml are actually supported by Harness.
    List<String> metricList = Arrays.asList(yaml.getMetrics().split(","));
    Map<String, Metric> metrics = DatadogState.metrics(metricList);
    if (metrics.size() != metricList.size()) {
      throw new WingsException("Invalid/Unsupported metrics found in the yaml: " + yaml.getMetrics());
    }
    bean.setMetrics(yaml.getMetrics());
    bean.setDatadogServiceName(yaml.getDatadogServiceName() == null ? "" : yaml.getDatadogServiceName());
    bean.setApplicationFilter(yaml.getApplicationFilter());
    bean.setStateType(StateType.DATA_DOG);
  }
}
