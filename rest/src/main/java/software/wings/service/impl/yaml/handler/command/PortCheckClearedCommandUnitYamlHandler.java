package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.PortCheckClearedCommandUnit;
import software.wings.beans.command.PortCheckClearedCommandUnit.Yaml;
import software.wings.beans.command.PortCheckClearedCommandUnit.Yaml.Builder;

/**
 * @author rktummala on 11/13/17
 */
public class PortCheckClearedCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected PortCheckClearedCommandUnit getCommandUnit() {
    return new PortCheckClearedCommandUnit();
  }
}
