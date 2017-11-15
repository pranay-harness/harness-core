package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.SetupEnvCommandUnit.Yaml;
import software.wings.beans.command.SetupEnvCommandUnit.Yaml.Builder;
import software.wings.beans.command.SetupEnvCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public class SetupEnvCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected SetupEnvCommandUnit getCommandUnit() {
    return new SetupEnvCommandUnit();
  }
}