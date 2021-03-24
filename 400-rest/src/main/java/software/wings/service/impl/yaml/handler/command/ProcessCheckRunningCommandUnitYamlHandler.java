package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ProcessCheckRunningCommandUnit;
import software.wings.beans.command.ProcessCheckRunningCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ProcessCheckRunningCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<ProcessCheckRunningCommandUnitYaml, ProcessCheckRunningCommandUnit> {
  @Override
  public Class getYamlClass() {
    return ProcessCheckRunningCommandUnitYaml.class;
  }

  @Override
  public ProcessCheckRunningCommandUnitYaml toYaml(ProcessCheckRunningCommandUnit bean, String appId) {
    ProcessCheckRunningCommandUnitYaml yaml = ProcessCheckRunningCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ProcessCheckRunningCommandUnit getCommandUnit() {
    return new ProcessCheckRunningCommandUnit();
  }
}
