package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.ProcessCheckStoppedCommandUnit;
import software.wings.beans.command.ProcessCheckStoppedCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ProcessCheckStoppedCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<Yaml, ProcessCheckStoppedCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(ProcessCheckStoppedCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ProcessCheckStoppedCommandUnit getCommandUnit() {
    return new ProcessCheckStoppedCommandUnit();
  }
}