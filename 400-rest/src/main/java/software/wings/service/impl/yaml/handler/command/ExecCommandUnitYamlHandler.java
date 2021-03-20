package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ExecCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ExecCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<ExecCommandUnitYaml, ExecCommandUnit> {
  @Override
  public ExecCommandUnitYaml toYaml(ExecCommandUnit bean, String appId) {
    ExecCommandUnitYaml yaml = ExecCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return ExecCommandUnitYaml.class;
  }

  @Override
  protected ExecCommandUnit getCommandUnit() {
    return new ExecCommandUnit();
  }
}
