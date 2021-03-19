package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.DockerStartCommandUnit;
import software.wings.beans.command.DockerStartCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class DockerStartCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<DockerStartCommandUnitYaml, DockerStartCommandUnit> {
  @Override
  public Class getYamlClass() {
    return DockerStartCommandUnitYaml.class;
  }

  @Override
  public DockerStartCommandUnitYaml toYaml(DockerStartCommandUnit bean, String appId) {
    DockerStartCommandUnitYaml yaml = DockerStartCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected DockerStartCommandUnit getCommandUnit() {
    return new DockerStartCommandUnit();
  }
}
