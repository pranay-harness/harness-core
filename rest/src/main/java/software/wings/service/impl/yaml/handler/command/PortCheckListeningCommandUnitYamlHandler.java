package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.PortCheckListeningCommandUnit.Yaml;
import software.wings.beans.command.PortCheckListeningCommandUnit.Yaml.Builder;
import software.wings.beans.command.PortCheckListeningCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public class PortCheckListeningCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected PortCheckListeningCommandUnit getCommandUnit() {
    return new PortCheckListeningCommandUnit();
  }
}
