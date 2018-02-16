package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureStrategy.FailureStrategyBuilder;
import software.wings.beans.FailureStrategy.Yaml;
import software.wings.beans.FailureType;
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
@Singleton
public class FailureStrategyYamlHandler extends BaseYamlHandler<FailureStrategy.Yaml, FailureStrategy> {
  private FailureStrategy toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    RepairActionCode repairActionCode = Util.getEnumFromString(RepairActionCode.class, yaml.getRepairActionCode());
    ExecutionScope executionScope = Util.getEnumFromString(ExecutionScope.class, yaml.getExecutionScope());
    RepairActionCode repairActionCodeAfterRetry =
        Util.getEnumFromString(RepairActionCode.class, yaml.getRepairActionCodeAfterRetry());

    FailureStrategyBuilder failureStrategyBuilder = FailureStrategyBuilder.aFailureStrategy()
                                                        .withExecutionScope(executionScope)
                                                        .withRepairActionCode(repairActionCode)
                                                        .withRepairActionCodeAfterRetry(repairActionCodeAfterRetry)
                                                        .withRetryCount(yaml.getRetryCount())
                                                        .withRetryIntervals(yaml.getRetryIntervals());

    if (isNotEmpty(yaml.getFailureTypes())) {
      yaml.getFailureTypes().stream().forEach(failureTypeString -> {
        FailureType failureType = Util.getEnumFromString(FailureType.class, failureTypeString);
        failureStrategyBuilder.addFailureTypes(failureType);
      });
    }
    return failureStrategyBuilder.build();
  }

  @Override
  public Yaml toYaml(FailureStrategy bean, String appId) {
    List<String> failureTypeList =
        bean.getFailureTypes().stream().map(failureType -> failureType.name()).collect(Collectors.toList());
    String repairActionCode = Util.getStringFromEnum(bean.getRepairActionCode());
    String repairActionCodeAfterRetry = Util.getStringFromEnum(bean.getRepairActionCodeAfterRetry());
    String executionScope = Util.getStringFromEnum(bean.getExecutionScope());

    return FailureStrategy.Yaml.Builder.anYaml()
        .withExecutionScope(executionScope)
        .withFailureTypes(failureTypeList)
        .withRepairActionCode(repairActionCode)
        .withRepairActionCodeAfterRetry(repairActionCodeAfterRetry)
        .withRetryCount(bean.getRetryCount())
        .withRetryIntervals(bean.getRetryIntervals())
        .build();
  }

  @Override
  public FailureStrategy upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
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
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // DO nothing
  }
}
