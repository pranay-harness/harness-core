package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.PortCheckListeningCommandUnit;
import software.wings.beans.command.PortCheckListeningCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class PortCheckListeningCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<PortCheckListeningCommandUnitYaml, PortCheckListeningCommandUnit> {
  @Override
  public Class getYamlClass() {
    return PortCheckListeningCommandUnitYaml.class;
  }

  @Override
  public PortCheckListeningCommandUnitYaml toYaml(PortCheckListeningCommandUnit bean, String appId) {
    PortCheckListeningCommandUnitYaml yaml = PortCheckListeningCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected PortCheckListeningCommandUnit getCommandUnit() {
    return new PortCheckListeningCommandUnit();
  }
}
