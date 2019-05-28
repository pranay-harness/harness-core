package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import io.harness.exception.WingsException;
import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatadogCvConfigurationYamlHandler
    extends CVConfigurationYamlHandler<DatadogCVConfigurationYaml, DatadogCVServiceConfiguration> {
  @Override
  public DatadogCVConfigurationYaml toYaml(DatadogCVServiceConfiguration bean, String appId) {
    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDatadogServiceName(bean.getDatadogServiceName());
    yaml.setDockerMetrics(bean.getDockerMetrics());
    yaml.setEcsMetrics(bean.getEcsMetrics());
    yaml.setCustomMetrics(bean.getCustomMetrics());
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
      cvConfigurationService.saveToDatabase(bean, true);
    }

    return bean;
  }

  private void toBean(
      DatadogCVServiceConfiguration bean, ChangeContext<DatadogCVConfigurationYaml> changeContext, String appId) {
    DatadogCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    List<String> metricList = new ArrayList<>();
    // validate if the metrics in yaml are actually supported by Harness.

    if (isNotEmpty(yaml.getDockerMetrics())) {
      yaml.getDockerMetrics().values().forEach(metric -> { metricList.addAll(Arrays.asList(metric.split(","))); });
      bean.setDockerMetrics(yaml.getDockerMetrics());
    }
    if (isNotEmpty(yaml.getEcsMetrics())) {
      yaml.getEcsMetrics().values().forEach(metric -> { metricList.addAll(Arrays.asList(metric.split(","))); });
      bean.setEcsMetrics(yaml.getEcsMetrics());
    }
    if (isNotEmpty(yaml.getCustomMetrics())) {
      bean.setCustomMetrics(yaml.getCustomMetrics());
    }

    if (isEmpty(yaml.getDatadogServiceName()) && isEmpty(yaml.getDockerMetrics()) && isEmpty(yaml.getEcsMetrics())
        && isEmpty(yaml.getCustomMetrics())) {
      throw new WingsException("No metrics found in the yaml");
    }

    Map<String, Metric> metrics = DatadogState.metrics(Optional.ofNullable(metricList), Optional.empty(),
        Optional.ofNullable(yaml.getCustomMetrics()), Optional.empty(), Optional.empty());
    if (metrics.size() != metricList.size()) {
      throw new WingsException("Invalid/Unsupported metrics found in the yaml");
    }

    bean.setDatadogServiceName(yaml.getDatadogServiceName() == null ? "" : yaml.getDatadogServiceName());
    bean.setStateType(StateType.DATA_DOG);
  }
}
