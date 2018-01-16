package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.beans.command.SetupEnvCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class SetupEnvCommandUnitYamlHandler extends AbstractExecCommandUnitYamlHandler<Yaml, SetupEnvCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(SetupEnvCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected SetupEnvCommandUnit getCommandUnit() {
    return new SetupEnvCommandUnit();
  }
}