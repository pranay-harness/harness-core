package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AwsLambdaCommandUnit.Yaml.Builder;
import software.wings.beans.command.CodeDeployCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public class CodeDeployCommandUnitYamlHandler
    extends CommandUnitYamlHandler<CodeDeployCommandUnit.Yaml, CodeDeployCommandUnit, Builder> {
  @Override
  public Class getYamlClass() {
    return CodeDeployCommandUnit.Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.aYaml();
  }

  @Override
  protected CodeDeployCommandUnit getCommandUnit() {
    return new CodeDeployCommandUnit();
  }
}
