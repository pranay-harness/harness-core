package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.PortCheckClearedCommandUnit;
import software.wings.beans.command.PortCheckClearedCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class PortCheckClearedCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<PortCheckClearedCommandUnitYaml, PortCheckClearedCommandUnit> {
  @Override
  public Class getYamlClass() {
    return PortCheckClearedCommandUnitYaml.class;
  }

  @Override
  public PortCheckClearedCommandUnitYaml toYaml(PortCheckClearedCommandUnit bean, String appId) {
    PortCheckClearedCommandUnitYaml yaml = PortCheckClearedCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected PortCheckClearedCommandUnit getCommandUnit() {
    return new PortCheckClearedCommandUnit();
  }
}
