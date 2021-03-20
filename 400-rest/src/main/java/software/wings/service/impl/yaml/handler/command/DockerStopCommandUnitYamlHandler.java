package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.DockerStopCommandUnit;
import software.wings.beans.command.DockerStopCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class DockerStopCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<DockerStopCommandUnitYaml, DockerStopCommandUnit> {
  @Override
  public Class getYamlClass() {
    return DockerStopCommandUnitYaml.class;
  }

  @Override
  public DockerStopCommandUnitYaml toYaml(DockerStopCommandUnit bean, String appId) {
    DockerStopCommandUnitYaml yaml = DockerStopCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected DockerStopCommandUnit getCommandUnit() {
    return new DockerStopCommandUnit();
  }
}
