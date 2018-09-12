package software.wings.service.impl.yaml.handler.workflow;

import static java.util.stream.Collectors.toList;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureStrategy.Yaml;
import software.wings.beans.FailureType;
import software.wings.beans.RepairActionCode;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Util;

import java.util.List;

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

    return FailureStrategy.builder()
        .executionScope(executionScope)
        .repairActionCode(repairActionCode)
        .repairActionCodeAfterRetry(repairActionCodeAfterRetry)
        .retryCount(yaml.getRetryCount())
        .retryIntervals(yaml.getRetryIntervals())
        .failureTypes(yaml.getFailureTypes() != null
                ? yaml.getFailureTypes()
                      .stream()
                      .map(failureTypeString -> Util.getEnumFromString(FailureType.class, failureTypeString))
                      .collect(toList())
                : null)
        .specificSteps(yaml.getSpecificSteps())
        .build();
  }

  @Override
  public Yaml toYaml(FailureStrategy bean, String appId) {
    List<String> failureTypeList = null;
    if (bean.getFailureTypes() != null) {
      failureTypeList = bean.getFailureTypes().stream().map(failureType -> failureType.name()).collect(toList());
    }
    String repairActionCode = Util.getStringFromEnum(bean.getRepairActionCode());
    String repairActionCodeAfterRetry = Util.getStringFromEnum(bean.getRepairActionCodeAfterRetry());
    String executionScope = Util.getStringFromEnum(bean.getExecutionScope());

    return FailureStrategy.Yaml.builder()
        .executionScope(executionScope)
        .failureTypes(failureTypeList)
        .repairActionCode(repairActionCode)
        .repairActionCodeAfterRetry(repairActionCodeAfterRetry)
        .retryCount(bean.getRetryCount())
        .retryIntervals(bean.getRetryIntervals())
        .specificSteps(bean.getSpecificSteps())
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
