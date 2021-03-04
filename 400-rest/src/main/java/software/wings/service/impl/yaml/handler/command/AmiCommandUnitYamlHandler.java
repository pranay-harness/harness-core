package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.AmiCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 12/29/17
 */
@Singleton
public class AmiCommandUnitYamlHandler extends CommandUnitYamlHandler<AmiCommandUnitYaml, AmiCommandUnit> {
  @Override
  public Class getYamlClass() {
    return AmiCommandUnitYaml.class;
  }

  @Override
  public AmiCommandUnitYaml toYaml(AmiCommandUnit bean, String appId) {
    AmiCommandUnitYaml yaml = AmiCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected AmiCommandUnit getCommandUnit() {
    return new AmiCommandUnit();
  }
}
