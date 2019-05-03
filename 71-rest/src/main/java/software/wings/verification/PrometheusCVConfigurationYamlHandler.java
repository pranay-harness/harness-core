package software.wings.verification;

import static software.wings.resources.PrometheusResource.validateTransactions;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.sm.StateType;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml;

import java.util.List;
import java.util.Map;

@Slf4j
public class PrometheusCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<PrometheusCVConfigurationYaml, PrometheusCVServiceConfiguration> {
  @Override
  public PrometheusCVConfigurationYaml toYaml(PrometheusCVServiceConfiguration bean, String appId) {
    PrometheusCVConfigurationYaml yaml = PrometheusCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(StateType.PROMETHEUS.name());

    try {
      List<TimeSeries> timeSeriesList = bean.getTimeSeriesToAnalyze();
      yaml.setTimeSeriesList(timeSeriesList);
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
    return yaml;
  }

  @Override
  public PrometheusCVServiceConfiguration upsertFromYaml(
      ChangeContext<PrometheusCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();

    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    PrometheusCVServiceConfiguration bean = PrometheusCVServiceConfiguration.builder().build();
    super.toBean(changeContext, bean, appId, yamlFilePath);

    PrometheusCVConfigurationYaml yaml = changeContext.getYaml();
    try {
      List<TimeSeries> timeSeriesList = yaml.getTimeSeriesList();
      validateTimeSeriesMetrics(timeSeriesList);
      bean.setTimeSeriesToAnalyze(timeSeriesList);
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }

    bean.setStateType(StateType.PROMETHEUS);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);
    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      cvConfigurationService.saveToDatabase(bean, true);
    }
    return bean;
  }

  @Override
  public Class getYamlClass() {
    return PrometheusCVConfigurationYaml.class;
  }

  @Override
  public PrometheusCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (PrometheusCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void validateTimeSeriesMetrics(List<TimeSeries> timeSeriesList) {
    Map<String, String> invalidFields = validateTransactions(timeSeriesList, false);
    if (EmptyPredicate.isNotEmpty(invalidFields)) {
      StringBuilder errorMsgBuilder = new StringBuilder();
      invalidFields.values().forEach(value -> {
        errorMsgBuilder.append(value);
        errorMsgBuilder.append('\n');
      });
      String errorMsg = errorMsgBuilder.toString();
      logger.error(errorMsg);
      throw new InvalidRequestException(errorMsg);
    }
  }
}
