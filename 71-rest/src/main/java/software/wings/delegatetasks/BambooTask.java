package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static software.wings.sm.states.BambooState.BambooExecutionResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTaskPackage;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.Result;
import software.wings.sm.states.ParameterEntry;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by sgurubelli on 8/29/17.
 */
@OwnedBy(CDC)
@Slf4j
public class BambooTask extends AbstractDelegateRunnableTask {
  @Inject private BambooService bambooService;

  public BambooTask(DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public BambooExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public BambooExecutionResponse run(Object[] parameters) {
    BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
    logger.info("In Bamboo Task run method");
    try {
      bambooExecutionResponse = run((BambooConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1],
          (String) parameters[2], (List<ParameterEntry>) parameters[3]);
    } catch (Exception e) {
      logger.warn("Failed to execute Bamboo verification task: " + ExceptionUtils.getMessage(e), e);
      bambooExecutionResponse.setExecutionStatus(ExecutionStatus.FAILED);
    }
    logger.info("Bamboo task  completed");
    return bambooExecutionResponse;
  }

  public BambooExecutionResponse run(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails,
      String planKey, List<ParameterEntry> parameterEntries) {
    BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;
    try {
      Map<String, String> evaluatedParameters = Maps.newLinkedHashMap();
      if (isNotEmpty(parameterEntries)) {
        parameterEntries.forEach(
            parameterEntry -> evaluatedParameters.put(parameterEntry.getKey(), parameterEntry.getValue()));
      }
      String buildResultKey = bambooService.triggerPlan(bambooConfig, encryptionDetails, planKey, evaluatedParameters);
      Result result = waitForBuildExecutionToFinish(bambooConfig, encryptionDetails, buildResultKey);
      String buildState = result.getBuildState();
      if (result == null || buildState == null) {
        executionStatus = ExecutionStatus.FAILED;
        logger.info("Bamboo execution failed for plan {}", planKey);
      } else {
        if (!"Successful".equalsIgnoreCase(buildState)) {
          executionStatus = ExecutionStatus.FAILED;
          logger.info("Build result for Bamboo url {}, plan key {}, build key {} is Failed. Result {}",
              bambooConfig.getBambooUrl(), planKey, buildResultKey, result);
        }
        bambooExecutionResponse.setProjectName(result.getProjectName());
        bambooExecutionResponse.setPlanName(result.getPlanName());
        bambooExecutionResponse.setBuildNumber(result.getBuildNumber());
        bambooExecutionResponse.setBuildStatus(result.getBuildState());
        bambooExecutionResponse.setBuildUrl(result.getBuildUrl());
        bambooExecutionResponse.setParameters(parameterEntries);
      }
    } catch (Exception e) {
      logger.warn("Failed to execute Bamboo verification task: " + ExceptionUtils.getMessage(e), e);
      errorMessage = ExceptionUtils.getMessage(e);
      executionStatus = ExecutionStatus.FAILED;
    }
    bambooExecutionResponse.setErrorMessage(errorMessage);
    bambooExecutionResponse.setExecutionStatus(executionStatus);
    return bambooExecutionResponse;
  }

  private Result waitForBuildExecutionToFinish(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String buildResultKey) {
    Result result;
    do {
      logger.info("Waiting for build execution {} to finish", buildResultKey);
      sleep(ofSeconds(5));
      result = bambooService.getBuildResult(bambooConfig, encryptionDetails, buildResultKey);
      logger.info("Build result for build key {} is {}", buildResultKey, result);
    } while (result.getBuildState() == null || result.getBuildState().equalsIgnoreCase("Unknown"));

    // Get the build result
    logger.info("Build execution for build key {} is finished. Result:{} ", buildResultKey, result);
    return result;
  }
}
