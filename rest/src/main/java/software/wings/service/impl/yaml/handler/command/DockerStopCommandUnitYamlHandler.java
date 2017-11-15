package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.DockerStopCommandUnit.Yaml;
import software.wings.beans.command.DockerStopCommandUnit.Yaml.Builder;
import software.wings.beans.command.DockerStopCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public class DockerStopCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected DockerStopCommandUnit getCommandUnit() {
    return new DockerStopCommandUnit();
  }
}
