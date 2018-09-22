package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.AmiCommandUnit.Yaml;

/**
 * @author rktummala on 12/29/17
 */
@Singleton
public class AmiCommandUnitYamlHandler extends CommandUnitYamlHandler<Yaml, AmiCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(AmiCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected AmiCommandUnit getCommandUnit() {
    return new AmiCommandUnit();
  }
}
