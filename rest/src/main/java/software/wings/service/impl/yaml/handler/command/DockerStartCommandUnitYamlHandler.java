package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.DockerStartCommandUnit;
import software.wings.beans.command.DockerStartCommandUnit.Yaml;
import software.wings.beans.command.DockerStartCommandUnit.Yaml.Builder;

/**
 * @author rktummala on 11/13/17
 */
public class DockerStartCommandUnitYamlHandler extends ExecCommandUnitYamlHandler {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected DockerStartCommandUnit getCommandUnit() {
    return new DockerStartCommandUnit();
  }
}
