package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.ProcessCheckRunningCommandUnit;
import software.wings.beans.command.ProcessCheckRunningCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ProcessCheckRunningCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<Yaml, ProcessCheckRunningCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(ProcessCheckRunningCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ProcessCheckRunningCommandUnit getCommandUnit() {
    return new ProcessCheckRunningCommandUnit();
  }
}
