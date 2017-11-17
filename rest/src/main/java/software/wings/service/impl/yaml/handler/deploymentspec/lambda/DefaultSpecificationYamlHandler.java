package software.wings.service.impl.yaml.handler.deploymentspec.lambda;

import software.wings.beans.ErrorCode;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.DefaultSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
public class DefaultSpecificationYamlHandler extends BaseYamlHandler<Yaml, LambdaSpecification.DefaultSpecification> {
  @Override
  public Yaml toYaml(LambdaSpecification.DefaultSpecification defaultSpecification, String appId) {
    return Yaml.builder()
        .memorySize(defaultSpecification.getMemorySize())
        .runtime(defaultSpecification.getRuntime())
        .timeout(defaultSpecification.getTimeout())
        .build();
  }

  @Override
  public LambdaSpecification.DefaultSpecification updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private LambdaSpecification.DefaultSpecification setWithYamlValues(ChangeContext<Yaml> changeContext)
      throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    return LambdaSpecification.DefaultSpecification.builder()
        .memorySize(yaml.getMemorySize())
        .runtime(yaml.getRuntime())
        .timeout(yaml.getTimeout())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public LambdaSpecification.DefaultSpecification createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public LambdaSpecification.DefaultSpecification get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
