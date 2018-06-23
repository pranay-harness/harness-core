package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static software.wings.sm.states.BambooState.BambooExecutionResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.Result;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.sm.states.ParameterEntry;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by sgurubelli on 8/29/17.
 */
public class BambooTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(BambooTask.class);

  @Inject private BambooService bambooService;

  public BambooTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public BambooExecutionResponse run(Object[] parameters) {
    BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
    logger.info("In Bamboo Task run method");
    try {
      bambooExecutionResponse = run((BambooConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1],
          (String) parameters[2], (List<ParameterEntry>) parameters[3], (List<FilePathAssertionEntry>) parameters[4]);
    } catch (Exception e) {
      logger.warn("Failed to execute Bamboo verification task: " + Misc.getMessage(e), e);
      bambooExecutionResponse.setExecutionStatus(ExecutionStatus.FAILED);
    }
    logger.info("Bamboo task  completed");
    return bambooExecutionResponse;
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  public BambooExecutionResponse run(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails,
      String planKey, List<ParameterEntry> parameterEntries, List<FilePathAssertionEntry> filePathAssertionEntries) {
    BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;
    try {
      Map<String, String> evaluatedParameters = Maps.newLinkedHashMap();
      if (isNotEmpty(parameterEntries)) {
        parameterEntries.forEach(
            parameterEntry -> { evaluatedParameters.put(parameterEntry.getKey(), parameterEntry.getValue()); });
      }
      String buildResultKey = bambooService.triggerPlan(bambooConfig, encryptionDetails, planKey, evaluatedParameters);
      // waitForBuildStartExecution(bambooConfig, buildResultKey);
      Result result = waitForBuildExecutionToFinish(bambooConfig, encryptionDetails, buildResultKey);
      String buildState = result.getBuildState();
      if (result == null || buildState == null) {
        executionStatus = ExecutionStatus.FAILED;
        logger.info("Bamboo execution failed for plan {}", planKey);
      } else {
        if (buildState != null) {
          if (!buildState.equalsIgnoreCase("Successful")) {
            executionStatus = ExecutionStatus.FAILED;
            logger.info("Build result for Bamboo url {}, plan key {}, build key {} is Failed. Result {}",
                bambooConfig.getBambooUrl(), planKey, buildResultKey, result);
          }
        }
        bambooExecutionResponse.setProjectName(result.getProjectName());
        bambooExecutionResponse.setPlanName(result.getPlanName());
        bambooExecutionResponse.setBuildNumber(result.getBuildNumber());
        bambooExecutionResponse.setBuildStatus(result.getBuildState());
        bambooExecutionResponse.setBuildUrl(result.getBuildUrl());
        bambooExecutionResponse.setParameters(parameterEntries);
      }
    } catch (Exception e) {
      logger.warn("Failed to execute Bamboo verification task: " + Misc.getMessage(e), e);
      errorMessage = Misc.getMessage(e);
      executionStatus = ExecutionStatus.FAILED;
    }
    bambooExecutionResponse.setErrorMessage(errorMessage);
    bambooExecutionResponse.setExecutionStatus(executionStatus);
    return bambooExecutionResponse;
  }

  private Result waitForBuildExecutionToFinish(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails,
      String buildResultKey) throws IOException {
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

  /*private Status waitForBuildStartExecution(BambooConfig bambooConfig, String buildResultKey) throws IOException {
    Status status;
    do {
      Misc.sleepWithRuntimeException(1000);
      status =  bambooService.getBuildResultStatus(bambooConfig, buildResultKey);
    } while (status == null);
    return status;
  }*/
}
