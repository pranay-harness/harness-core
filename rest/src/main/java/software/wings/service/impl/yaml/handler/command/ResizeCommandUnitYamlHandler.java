package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ResizeCommandUnit;
import software.wings.beans.command.ResizeCommandUnit.Yaml;
import software.wings.beans.command.ResizeCommandUnit.Yaml.Builder;

/**
 * @author rktummala on 11/13/17
 */
public class ResizeCommandUnitYamlHandler extends CommandUnitYamlHandler<Yaml, ResizeCommandUnit, Builder> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.aYaml();
  }

  @Override
  protected ResizeCommandUnit getCommandUnit() {
    return new ResizeCommandUnit();
  }
}
