package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.ResizeCommandUnit;
import software.wings.beans.command.ResizeCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ResizeCommandUnitYamlHandler extends ContainerResizeCommandUnitYamlHandler<Yaml, ResizeCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(ResizeCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ResizeCommandUnit getCommandUnit() {
    return new ResizeCommandUnit();
  }
}
