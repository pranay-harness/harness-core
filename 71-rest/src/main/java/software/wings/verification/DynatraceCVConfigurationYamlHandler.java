package software.wings.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration.DynaTraceCVConfigurationYaml;

import java.util.List;

public class DynatraceCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<DynaTraceCVConfigurationYaml, DynaTraceCVServiceConfiguration> {
  @Override
  public DynaTraceCVConfigurationYaml toYaml(DynaTraceCVServiceConfiguration bean, String appId) {
    DynaTraceCVConfigurationYaml yaml = DynaTraceCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);

    yaml.setType(StateType.DYNA_TRACE.name());
    return yaml;
  }

  @Override
  public DynaTraceCVServiceConfiguration upsertFromYaml(
      ChangeContext<DynaTraceCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    DynaTraceCVServiceConfiguration bean = DynaTraceCVServiceConfiguration.builder().build();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setStateType(StateType.DYNA_TRACE);

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
    return DynaTraceCVConfigurationYaml.class;
  }

  @Override
  public DynaTraceCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (DynaTraceCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }
}
