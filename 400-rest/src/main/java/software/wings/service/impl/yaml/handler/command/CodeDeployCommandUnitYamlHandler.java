package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.CodeDeployCommandUnit;
import software.wings.beans.command.CodeDeployCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class CodeDeployCommandUnitYamlHandler
    extends CommandUnitYamlHandler<CodeDeployCommandUnit.Yaml, CodeDeployCommandUnit> {
  @Override
  public Class getYamlClass() {
    return CodeDeployCommandUnit.Yaml.class;
  }

  @Override
  public Yaml toYaml(CodeDeployCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected CodeDeployCommandUnit getCommandUnit() {
    return new CodeDeployCommandUnit();
  }
}
