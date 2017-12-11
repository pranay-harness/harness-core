package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ProcessCheckRunningCommandUnit.Yaml;
import software.wings.beans.command.ProcessCheckRunningCommandUnit.Yaml.Builder;
import software.wings.beans.command.ProcessCheckRunningCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public class ProcessCheckRunningCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected ProcessCheckRunningCommandUnit getCommandUnit() {
    return new ProcessCheckRunningCommandUnit();
  }
}
