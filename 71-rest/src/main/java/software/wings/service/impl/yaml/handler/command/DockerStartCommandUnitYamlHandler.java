package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.DockerStartCommandUnit;
import software.wings.beans.command.DockerStartCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class DockerStartCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<Yaml, DockerStartCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(DockerStartCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected DockerStartCommandUnit getCommandUnit() {
    return new DockerStartCommandUnit();
  }
}
