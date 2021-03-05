package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.beans.command.SetupEnvCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class SetupEnvCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<SetupEnvCommandUnitYaml, SetupEnvCommandUnit> {
  @Override
  public Class getYamlClass() {
    return SetupEnvCommandUnitYaml.class;
  }

  @Override
  public SetupEnvCommandUnitYaml toYaml(SetupEnvCommandUnit bean, String appId) {
    SetupEnvCommandUnitYaml yaml = SetupEnvCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected SetupEnvCommandUnit getCommandUnit() {
    return new SetupEnvCommandUnit();
  }
}
