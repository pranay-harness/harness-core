package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureStrategy.Yaml;
import software.wings.beans.RepairActionCode;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
public class FailureStrategyYamlHandler extends BaseYamlHandler<FailureStrategy.Yaml, FailureStrategy> {
  @Override
  public FailureStrategy createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private FailureStrategy setWithYamlValues(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    RepairActionCode repairActionCode = Util.getEnumFromString(RepairActionCode.class, yaml.getRepairActionCode());
    ExecutionScope executionScope = Util.getEnumFromString(ExecutionScope.class, yaml.getExecutionScope());
    RepairActionCode repairActionCodeAfterRetry =
        Util.getEnumFromString(RepairActionCode.class, yaml.getRepairActionCodeAfterRetry());
    return FailureStrategy.FailureStrategyBuilder.aFailureStrategy()
        .withExecutionScope(executionScope)
        .withRepairActionCode(repairActionCode)
        .withRepairActionCodeAfterRetry(repairActionCodeAfterRetry)
        .withRetryCount(yaml.getRetryCount())
        .withRetryIntervals(yaml.getRetryIntervals())
        .build();
  }

  @Override
  public Yaml toYaml(FailureStrategy bean, String appId) {
    List<String> failureTypeList =
        bean.getFailureTypes().stream().map(failureType -> failureType.name()).collect(Collectors.toList());
    return FailureStrategy.Yaml.Builder.anYaml()
        .withExecutionScope(bean.getExecutionScope() != null ? bean.getExecutionScope().name() : null)
        .withFailureTypes(failureTypeList)
        .withRepairActionCode(bean.getRepairActionCode().name())
        .withRepairActionCodeAfterRetry(
            bean.getRepairActionCodeAfterRetry() != null ? bean.getRepairActionCodeAfterRetry().name() : null)
        .withRetryCount(bean.getRetryCount())
        .withRetryIntervals(bean.getRetryIntervals())
        .build();
  }

  @Override
  public FailureStrategy updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return FailureStrategy.Yaml.class;
  }

  @Override
  public FailureStrategy get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public FailureStrategy update(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
