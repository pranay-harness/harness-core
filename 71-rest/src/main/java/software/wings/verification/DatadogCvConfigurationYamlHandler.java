package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml.YamlMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DatadogCvConfigurationYamlHandler
    extends CVConfigurationYamlHandler<DatadogCVConfigurationYaml, DatadogCVServiceConfiguration> {
  @Override
  public DatadogCVConfigurationYaml toYaml(DatadogCVServiceConfiguration bean, String appId) {
    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setDatadogServiceName(bean.getDatadogServiceName());
    yaml.setDockerMetrics(bean.getDockerMetrics());
    yaml.setEcsMetrics(bean.getEcsMetrics());
    yaml.setCustomMetrics(convertToYamlCustomMetrics(bean.getCustomMetrics()));
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
    String appId = getAppId(changeContext);
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    DatadogCVServiceConfiguration bean = DatadogCVServiceConfiguration.builder().build();
    toBean(bean, changeContext, appId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      bean.setUuid(generateUuid());
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
      yaml.getDockerMetrics().values().forEach(metric -> metricList.addAll(Arrays.asList(metric.split(","))));
      bean.setDockerMetrics(yaml.getDockerMetrics());
    }
    if (isNotEmpty(yaml.getEcsMetrics())) {
      yaml.getEcsMetrics().values().forEach(metric -> metricList.addAll(Arrays.asList(metric.split(","))));
      bean.setEcsMetrics(yaml.getEcsMetrics());
    }
    if (isNotEmpty(yaml.getCustomMetrics())) {
      yaml.getCustomMetrics().values().forEach(yamlMetricList -> yamlMetricList.forEach(yamlMetric -> {
        metricList.addAll(Arrays.asList(yamlMetric.getMetricName()));
      }));
      bean.setCustomMetrics(convertToBeanCustomMetrics(yaml.getCustomMetrics()));
    }

    if (isEmpty(yaml.getDatadogServiceName()) && isEmpty(yaml.getDockerMetrics()) && isEmpty(yaml.getEcsMetrics())
        && isEmpty(yaml.getCustomMetrics())) {
      throw new VerificationOperationException(ErrorCode.APM_CONFIGURATION_ERROR, "No metrics found in the yaml");
    }

    Map<String, Metric> metrics = DatadogState.metrics(Optional.ofNullable(metricList), Optional.empty(),
        Optional.ofNullable(convertToBeanCustomMetrics(yaml.getCustomMetrics())), Optional.empty(), Optional.empty());
    if (metrics.size() != metricList.size()) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Invalid/Unsupported metrics found in the yaml");
    }

    bean.setDatadogServiceName(yaml.getDatadogServiceName() == null ? "" : yaml.getDatadogServiceName());
    bean.setStateType(StateType.DATA_DOG);
  }

  private Map<String, Set<Metric>> convertToBeanCustomMetrics(Map<String, List<YamlMetric>> yamlCustomMetrics) {
    Map<String, Set<Metric>> beanCustomMetrics = new HashMap<>();
    if (isNotEmpty(yamlCustomMetrics)) {
      yamlCustomMetrics.forEach((k, v) -> {
        Set<Metric> beanMetricList = new HashSet<>();
        if (isNotEmpty(v)) {
          v.forEach(yamlMetric -> beanMetricList.add(yamlMetric.convertToDatadogMetric()));
        }
        beanCustomMetrics.put(k, beanMetricList);
      });
    }
    return beanCustomMetrics;
  }

  private Map<String, List<YamlMetric>> convertToYamlCustomMetrics(Map<String, Set<Metric>> beanCustomMetrics) {
    Map<String, List<YamlMetric>> yamlCustomMetrics = new HashMap<>();
    if (isNotEmpty(beanCustomMetrics)) {
      beanCustomMetrics.forEach((k, v) -> {
        List<YamlMetric> yamlMetricList = new ArrayList<>();
        if (isNotEmpty(v)) {
          v.forEach(metric -> yamlMetricList.add(YamlMetric.convertToYamlMetric(metric)));
        }
        yamlCustomMetrics.put(k, yamlMetricList);
      });
    }
    return yamlCustomMetrics;
  }
}
