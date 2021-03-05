package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ProcessCheckStoppedCommandUnit;
import software.wings.beans.command.ProcessCheckStoppedCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ProcessCheckStoppedCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<ProcessCheckStoppedCommandUnitYaml, ProcessCheckStoppedCommandUnit> {
  @Override
  public Class getYamlClass() {
    return ProcessCheckStoppedCommandUnitYaml.class;
  }

  @Override
  public ProcessCheckStoppedCommandUnitYaml toYaml(ProcessCheckStoppedCommandUnit bean, String appId) {
    ProcessCheckStoppedCommandUnitYaml yaml = ProcessCheckStoppedCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ProcessCheckStoppedCommandUnit getCommandUnit() {
    return new ProcessCheckStoppedCommandUnit();
  }
}
