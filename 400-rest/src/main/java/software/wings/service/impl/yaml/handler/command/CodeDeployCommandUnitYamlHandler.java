package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.CodeDeployCommandUnit;
import software.wings.beans.command.CodeDeployCommandUnitYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class CodeDeployCommandUnitYamlHandler
    extends CommandUnitYamlHandler<CodeDeployCommandUnitYaml, CodeDeployCommandUnit> {
  @Override
  public Class getYamlClass() {
    return CodeDeployCommandUnitYaml.class;
  }

  @Override
  public CodeDeployCommandUnitYaml toYaml(CodeDeployCommandUnit bean, String appId) {
    CodeDeployCommandUnitYaml yaml = CodeDeployCommandUnitYaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected CodeDeployCommandUnit getCommandUnit() {
    return new CodeDeployCommandUnit();
  }
}
