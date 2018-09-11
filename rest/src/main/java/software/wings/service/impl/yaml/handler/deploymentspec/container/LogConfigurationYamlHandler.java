package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.LogConfiguration.LogOption;
import software.wings.beans.container.LogConfiguration.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.Collections;
import java.util.List;
/**
 * @author rktummala on 11/15/17
 */
@Singleton
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
    return toBean(changeContext);
  }

  private List<NameValuePair.Yaml> getLogOptionsYaml(List<LogOption> logOptionList) {
    if (isEmpty(logOptionList)) {
      return Collections.emptyList();
    }
    return logOptionList.stream()
        .map(logOption -> NameValuePair.Yaml.builder().name(logOption.getKey()).value(logOption.getValue()).build())
        .collect(toList());
  }

  private List<LogOption> getLogOptions(List<NameValuePair.Yaml> yamlList) {
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

  private LogConfiguration toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    return LogConfiguration.builder().logDriver(yaml.getLogDriver()).options(getLogOptions(yaml.getOptions())).build();
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
