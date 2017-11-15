package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.AwsLambdaCommandUnit.Yaml.Builder;

/**
 * @author rktummala on 11/13/17
 */
public class AwsLambdaCommandUnitYamlHandler
    extends CommandUnitYamlHandler<AwsLambdaCommandUnit.Yaml, AwsLambdaCommandUnit, AwsLambdaCommandUnit.Yaml.Builder> {
  @Override
  public Class getYamlClass() {
    return AwsLambdaCommandUnit.Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return AwsLambdaCommandUnit.Yaml.Builder.aYaml();
  }

  @Override
  protected AwsLambdaCommandUnit getCommandUnit() {
    return new AwsLambdaCommandUnit();
  }
}
