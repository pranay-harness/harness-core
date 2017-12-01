package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.beans.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.LogConfiguration.LogOption;
import software.wings.beans.container.LogConfiguration.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/15/17
 */
public class LogConfigurationYamlHandler extends BaseYamlHandler<Yaml, LogConfiguration> {
  @Override
  public Yaml toYaml(LogConfiguration logConfiguration, String appId) {
    return Yaml.builder()
        .logDriver(logConfiguration.getLogDriver())
        .options(getLogOptionsYaml(logConfiguration.getOptions()))
        .build();
  }

  @Override
  public LogConfiguration upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private List<NameValuePair.Yaml> getLogOptionsYaml(List<LogOption> logOptionList) {
    if (Util.isEmpty(logOptionList)) {
      return Collections.emptyList();
    }
    return logOptionList.stream()
        .map(logOption
            -> NameValuePair.Yaml.Builder.aYaml().withName(logOption.getKey()).withValue(logOption.getValue()).build())
        .collect(Collectors.toList());
  }

  private List<LogOption> getLogOptions(List<NameValuePair.Yaml> yamlList) {
    if (Util.isEmpty(yamlList)) {
      return Collections.emptyList();
    }

    return yamlList.stream()
        .map(yaml -> {
          LogOption logOption = new LogOption();
          logOption.setKey(yaml.getName());
          logOption.setValue(yaml.getValue());
          return logOption;
        })
        .collect(Collectors.toList());
  }

  @Override
  public LogConfiguration updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private LogConfiguration setWithYamlValues(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    return LogConfiguration.builder().logDriver(yaml.getLogDriver()).options(getLogOptions(yaml.getOptions())).build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public LogConfiguration createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public LogConfiguration get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
