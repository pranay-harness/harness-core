package software.wings.service.impl.yaml.handler.deploymentspec.lambda;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.LambdaSpecificationFunctionSpecificationYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class FunctionSpecificationYamlHandler
    extends BaseYamlHandler<LambdaSpecificationFunctionSpecificationYaml, FunctionSpecification> {
  @Override
  public LambdaSpecificationFunctionSpecificationYaml toYaml(
      FunctionSpecification functionSpecification, String appId) {
    return LambdaSpecificationFunctionSpecificationYaml.builder()
        .functionName(functionSpecification.getFunctionName())
        .handler(functionSpecification.getHandler())
        .memorySize(functionSpecification.getMemorySize())
        .runtime(functionSpecification.getRuntime())
        .timeout(functionSpecification.getTimeout())
        .build();
  }

  @Override
  public FunctionSpecification upsertFromYaml(ChangeContext<LambdaSpecificationFunctionSpecificationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    return toBean(changeContext);
  }

  private FunctionSpecification toBean(ChangeContext<LambdaSpecificationFunctionSpecificationYaml> changeContext) {
    LambdaSpecificationFunctionSpecificationYaml yaml = changeContext.getYaml();

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
    return LambdaSpecificationFunctionSpecificationYaml.class;
  }

  @Override
  public FunctionSpecification get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<LambdaSpecificationFunctionSpecificationYaml> changeContext) {
    // do nothing
  }
}
