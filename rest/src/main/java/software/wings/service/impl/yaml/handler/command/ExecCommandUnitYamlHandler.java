package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ExecCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ExecCommandUnitYamlHandler extends AbstractExecCommandUnitYamlHandler<Yaml, ExecCommandUnit> {
  @Override
  public Yaml toYaml(ExecCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected ExecCommandUnit getCommandUnit() {
    return new ExecCommandUnit();
  }
}
