package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.PortCheckClearedCommandUnit;
import software.wings.beans.command.PortCheckClearedCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class PortCheckClearedCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<Yaml, PortCheckClearedCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(PortCheckClearedCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected PortCheckClearedCommandUnit getCommandUnit() {
    return new PortCheckClearedCommandUnit();
  }
}
