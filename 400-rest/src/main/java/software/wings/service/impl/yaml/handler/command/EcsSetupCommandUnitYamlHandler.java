package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.EcsSetupCommandUnit;
import software.wings.beans.command.EcsSetupCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author brett on 11/28/17
 */
@Singleton
public class EcsSetupCommandUnitYamlHandler
    extends ContainerSetupCommandUnitYamlHandler<EcsSetupCommandUnitYaml, EcsSetupCommandUnit> {
  @Override
  public Class getYamlClass() {
    return EcsSetupCommandUnitYaml.class;
  }

  @Override
  public EcsSetupCommandUnitYaml toYaml(EcsSetupCommandUnit bean, String appId) {
    EcsSetupCommandUnitYaml yaml = EcsSetupCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected EcsSetupCommandUnit getCommandUnit() {
    return new EcsSetupCommandUnit();
  }
}
