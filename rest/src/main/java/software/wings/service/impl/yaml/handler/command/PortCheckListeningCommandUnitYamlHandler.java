package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.PortCheckListeningCommandUnit;
import software.wings.beans.command.PortCheckListeningCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
public class PortCheckListeningCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<Yaml, PortCheckListeningCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(PortCheckListeningCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected PortCheckListeningCommandUnit getCommandUnit() {
    return new PortCheckListeningCommandUnit();
  }
}
