package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ProcessCheckStoppedCommandUnit.Yaml;
import software.wings.beans.command.ProcessCheckStoppedCommandUnit.Yaml.Builder;
import software.wings.beans.command.ProcessCheckStoppedCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public class ProcessCheckStoppedCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected ProcessCheckStoppedCommandUnit getCommandUnit() {
    return new ProcessCheckStoppedCommandUnit();
  }
}