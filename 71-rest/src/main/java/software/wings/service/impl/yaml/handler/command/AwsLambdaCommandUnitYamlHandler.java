package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Singleton;

import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.AwsLambdaCommandUnit.Yaml;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class AwsLambdaCommandUnitYamlHandler
    extends CommandUnitYamlHandler<AwsLambdaCommandUnit.Yaml, AwsLambdaCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(AwsLambdaCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected AwsLambdaCommandUnit getCommandUnit() {
    return new AwsLambdaCommandUnit();
  }
}
