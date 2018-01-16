package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.DockerStopCommandUnit;
import software.wings.beans.command.DockerStopCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class DockerStopCommandUnitYamlHandler extends AbstractExecCommandUnitYamlHandler<Yaml, DockerStopCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(DockerStopCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected DockerStopCommandUnit getCommandUnit() {
    return new DockerStopCommandUnit();
  }
}
