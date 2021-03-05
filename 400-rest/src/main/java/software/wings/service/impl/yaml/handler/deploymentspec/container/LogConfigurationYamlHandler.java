package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.NameValuePairYaml;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.LogConfiguration.LogOption;
import software.wings.beans.container.LogConfigurationYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class LogConfigurationYamlHandler extends BaseYamlHandler<LogConfigurationYaml, LogConfiguration> {
  @Override
  public LogConfigurationYaml toYaml(LogConfiguration logConfiguration, String appId) {
    return LogConfigurationYaml.builder()
        .logDriver(logConfiguration.getLogDriver())
        .options(getLogOptionsYaml(logConfiguration.getOptions()))
        .build();
  }

  @Override
  public LogConfiguration upsertFromYaml(
      ChangeContext<LogConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  private List<NameValuePairYaml> getLogOptionsYaml(List<LogOption> logOptionList) {
    if (isEmpty(logOptionList)) {
      return Collections.emptyList();
    }
    return logOptionList.stream()
        .map(logOption -> NameValuePairYaml.builder().name(logOption.getKey()).value(logOption.getValue()).build())
        .collect(toList());
  }

  private List<LogOption> getLogOptions(List<NameValuePairYaml> yamlList) {
    if (isEmpty(yamlList)) {
      return Collections.emptyList();
    }

    return yamlList.stream()
        .map(yaml -> {
          LogOption logOption = new LogOption();
          logOption.setKey(yaml.getName());
          logOption.setValue(yaml.getValue());
          return logOption;
        })
        .collect(toList());
  }

  private LogConfiguration toBean(ChangeContext<LogConfigurationYaml> changeContext) {
    LogConfigurationYaml yaml = changeContext.getYaml();

    return LogConfiguration.builder().logDriver(yaml.getLogDriver()).options(getLogOptions(yaml.getOptions())).build();
  }

  @Override
  public Class getYamlClass() {
    return LogConfigurationYaml.class;
  }

  @Override
  public LogConfiguration get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<LogConfigurationYaml> changeContext) {
    // Do nothing
  }
}
