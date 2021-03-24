package software.wings.service.impl.yaml.handler.deploymentspec.lambda;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.DefaultSpecification;
import software.wings.beans.LambdaSpecificationDefaultSpecificationYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class DefaultSpecificationYamlHandler
    extends BaseYamlHandler<LambdaSpecificationDefaultSpecificationYaml, LambdaSpecification.DefaultSpecification> {
  @Override
  public LambdaSpecificationDefaultSpecificationYaml toYaml(
      LambdaSpecification.DefaultSpecification defaultSpecification, String appId) {
    return LambdaSpecificationDefaultSpecificationYaml.builder()
        .memorySize(defaultSpecification.getMemorySize())
        .runtime(defaultSpecification.getRuntime())
        .timeout(defaultSpecification.getTimeout())
        .build();
  }

  @Override
  public DefaultSpecification upsertFromYaml(
      ChangeContext<LambdaSpecificationDefaultSpecificationYaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  private LambdaSpecification.DefaultSpecification toBean(
      ChangeContext<LambdaSpecificationDefaultSpecificationYaml> changeContext) {
    LambdaSpecificationDefaultSpecificationYaml yaml = changeContext.getYaml();
    return LambdaSpecification.DefaultSpecification.builder()
        .memorySize(yaml.getMemorySize())
        .runtime(yaml.getRuntime())
        .timeout(yaml.getTimeout())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return LambdaSpecificationDefaultSpecificationYaml.class;
  }

  @Override
  public LambdaSpecification.DefaultSpecification get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<LambdaSpecificationDefaultSpecificationYaml> changeContext) {
    // Do nothing
  }
}
