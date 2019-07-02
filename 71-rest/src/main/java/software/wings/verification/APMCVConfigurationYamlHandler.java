package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import io.harness.exception.WingsException;
import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;
import software.wings.verification.apm.APMCVServiceConfiguration;
import software.wings.verification.apm.APMCVServiceConfiguration.APMCVConfigurationYaml;

import java.util.List;

public class APMCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<APMCVServiceConfiguration.APMCVConfigurationYaml, APMCVServiceConfiguration> {
  @Override
  public APMCVConfigurationYaml toYaml(APMCVServiceConfiguration bean, String appId) {
    APMCVConfigurationYaml yaml = APMCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setMetricCollectionInfos(bean.getMetricCollectionInfos());
    yaml.setType(StateType.APM_VERIFICATION.name());
    return yaml;
  }

  @Override
  public APMCVServiceConfiguration upsertFromYaml(
      ChangeContext<APMCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();

    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    APMCVServiceConfiguration bean = APMCVServiceConfiguration.builder().build();
    super.toBean(changeContext, bean, appId, yamlFilePath);

    APMCVConfigurationYaml yaml = changeContext.getYaml();
    List<MetricCollectionInfo> metrics = yaml.getMetricCollectionInfos();

    if (isEmpty(metrics)) {
      throw new WingsException("No metrics found in the yaml");
    }

    bean.setMetricCollectionInfos(metrics);
    bean.setStateType(StateType.APM_VERIFICATION);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      bean.setUuid(generateUuid());
      cvConfigurationService.saveToDatabase(bean, true);
    }

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return APMCVConfigurationYaml.class;
  }

  @Override
  public APMCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (APMCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }
}
