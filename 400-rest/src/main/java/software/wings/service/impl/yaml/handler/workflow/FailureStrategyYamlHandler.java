package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;
import static io.harness.interrupts.ExecutionInterruptType.END_EXECUTION;
import static io.harness.interrupts.ExecutionInterruptType.IGNORE;
import static io.harness.interrupts.ExecutionInterruptType.MARK_SUCCESS;
import static io.harness.interrupts.ExecutionInterruptType.ROLLBACK;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.FailureType;
import io.harness.exception.HarnessException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.RepairActionCode;

import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureStrategyYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Utils;

import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@OwnedBy(CDC)
@Singleton
public class FailureStrategyYamlHandler extends BaseYamlHandler<FailureStrategyYaml, FailureStrategy> {
  private FailureStrategy toBean(ChangeContext<FailureStrategyYaml> changeContext) {
    FailureStrategyYaml yaml = changeContext.getYaml();
    RepairActionCode repairActionCode = Utils.getEnumFromString(RepairActionCode.class, yaml.getRepairActionCode());
    ExecutionScope executionScope = Utils.getEnumFromString(ExecutionScope.class, yaml.getExecutionScope());
    RepairActionCode repairActionCodeAfterRetry =
        Utils.getEnumFromString(RepairActionCode.class, yaml.getRepairActionCodeAfterRetry());
    ExecutionInterruptType actionAfterTimeout =
        Utils.getEnumFromString(ExecutionInterruptType.class, yaml.getActionAfterTimeout());
    Long manualInterventionTimeout = yaml.getManualInterventionTimeout();

    boolean isManualIntervention = RepairActionCode.MANUAL_INTERVENTION.equals(repairActionCode);

    if (isManualIntervention) {
      if (manualInterventionTimeout == null || manualInterventionTimeout < 60000) {
        throw new InvalidArgumentsException("\"manualInterventionTimeout\" should not be less than 1m (60000)");
      }
      List<ExecutionInterruptType> allowedActions =
          Arrays.asList(ABORT_ALL, END_EXECUTION, IGNORE, MARK_SUCCESS, ROLLBACK);
      if (!allowedActions.contains(actionAfterTimeout)) {
        throw new InvalidArgumentsException(String.format(
            "\"actionAfterTimeout\" should not be empty. Please provide valid value: %s", allowedActions));
      }
    }

    return FailureStrategy.builder()
        .executionScope(executionScope)
        .repairActionCode(repairActionCode)
        .repairActionCodeAfterRetry(repairActionCodeAfterRetry)
        .retryCount(yaml.getRetryCount())
        .retryIntervals(yaml.getRetryIntervals())
        .failureTypes(yaml.getFailureTypes() != null
                ? yaml.getFailureTypes()
                      .stream()
                      .map(failureTypeString -> Utils.getEnumFromString(FailureType.class, failureTypeString))
                      .collect(toList())
                : null)
        .specificSteps(yaml.getSpecificSteps())
        .actionAfterTimeout(isManualIntervention ? actionAfterTimeout : null)
        .manualInterventionTimeout(isManualIntervention ? manualInterventionTimeout : null)
        .build();
  }

  @Override
  public FailureStrategyYaml toYaml(FailureStrategy bean, String appId) {
    List<String> failureTypeList = null;
    if (bean.getFailureTypes() != null) {
      failureTypeList = bean.getFailureTypes().stream().map(Enum::name).collect(toList());
    }
    String repairActionCode = Utils.getStringFromEnum(bean.getRepairActionCode());
    String repairActionCodeAfterRetry = Utils.getStringFromEnum(bean.getRepairActionCodeAfterRetry());
    String executionScope = Utils.getStringFromEnum(bean.getExecutionScope());
    String actionAfterTimeout = Utils.getStringFromEnum(bean.getActionAfterTimeout());

    return FailureStrategyYaml.builder()
        .executionScope(executionScope)
        .failureTypes(failureTypeList)
        .repairActionCode(repairActionCode)
        .repairActionCodeAfterRetry(repairActionCodeAfterRetry)
        .retryCount(bean.getRetryCount())
        .retryIntervals(bean.getRetryIntervals())
        .specificSteps(bean.getSpecificSteps())
        .actionAfterTimeout(actionAfterTimeout)
        .manualInterventionTimeout(bean.getManualInterventionTimeout())
        .build();
  }

  @Override
  public FailureStrategy upsertFromYaml(
      ChangeContext<FailureStrategyYaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return FailureStrategyYaml.class;
  }

  @Override
  public FailureStrategy get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<FailureStrategyYaml> changeContext) {
    // DO nothing
  }
}
