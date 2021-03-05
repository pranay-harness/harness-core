package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ResizeCommandUnit;
import software.wings.beans.command.ResizeCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ResizeCommandUnitYamlHandler
    extends ContainerResizeCommandUnitYamlHandler<ResizeCommandUnitYaml, ResizeCommandUnit> {
  @Override
  public Class getYamlClass() {
    return ResizeCommandUnitYaml.class;
  }

  @Override
  public ResizeCommandUnitYaml toYaml(ResizeCommandUnit bean, String appId) {
    ResizeCommandUnitYaml yaml = ResizeCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ResizeCommandUnit getCommandUnit() {
    return new ResizeCommandUnit();
  }
}
