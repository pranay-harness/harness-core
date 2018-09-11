package software.wings.service.impl.yaml.handler.deploymentspec.lambda;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class FunctionSpecificationYamlHandler extends BaseYamlHandler<Yaml, FunctionSpecification> {
  @Override
  public Yaml toYaml(FunctionSpecification functionSpecification, String appId) {
    return Yaml.builder()
        .functionName(functionSpecification.getFunctionName())
        .handler(functionSpecification.getHandler())
        .memorySize(functionSpecification.getMemorySize())
        .runtime(functionSpecification.getRuntime())
        .timeout(functionSpecification.getTimeout())
        .build();
  }

  @Override
  public FunctionSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  private FunctionSpecification toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    return FunctionSpecification.builder()
        .functionName(yaml.getFunctionName())
        .handler(yaml.getHandler())
        .memorySize(yaml.getMemorySize())
        .runtime(yaml.getRuntime())
        .timeout(yaml.getTimeout())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public FunctionSpecification get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // do nothing
  }
}
