package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;
import static io.harness.threading.Morpheus.sleep;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.service.impl.LogServiceImpl.NUM_OF_LOGS_TO_KEEP;

import com.google.inject.Inject;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.client.HttpResponseException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.JenkinsTaskParams;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rishi on 12/14/16.
 */
@OwnedBy(CDC)
@Slf4j
public class JenkinsTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private JenkinsUtils jenkinsUtil;

  public JenkinsTask(DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public JenkinsExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public JenkinsExecutionResponse run(Object[] parameters) {
    return run((JenkinsTaskParams) parameters[0]);
  }

  public JenkinsExecutionResponse run(JenkinsTaskParams jenkinsTaskParams) {
    JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;

    JenkinsConfig jenkinsConfig = jenkinsTaskParams.getJenkinsConfig();
    encryptionService.decrypt(jenkinsConfig, jenkinsTaskParams.getEncryptedDataDetails());
    Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);
    String msg = "Error occurred while starting Jenkins task\n";

    switch (jenkinsTaskParams.getSubTaskType()) {
      case START_TASK:
        try {
          logger.info("In Jenkins Task Triggering Job {}", jenkinsTaskParams.getJobName());
          logService.save(getAccountId(),
              constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                  jenkinsTaskParams.getAppId(), LogLevel.INFO,
                  "Triggering Jenkins Job : " + jenkinsTaskParams.getJobName(), RUNNING));

          QueueReference queueItem = jenkins.trigger(jenkinsTaskParams.getJobName(), jenkinsTaskParams.getParameters());
          logger.info("Triggered Job successfully and queued Build  URL {} ",
              queueItem == null ? null : queueItem.getQueueItemUrlPart());

          // Check if start jenkins is success
          if (queueItem != null && isNotEmpty(queueItem.getQueueItemUrlPart())) {
            jenkinsExecutionResponse.setQueuedBuildUrl(queueItem.getQueueItemUrlPart());

            logService.save(getAccountId(),
                constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                    jenkinsTaskParams.getAppId(), LogLevel.INFO,
                    "Triggered Job successfully and queued Build  URL : " + queueItem.getQueueItemUrlPart()
                        + " and remaining Time (sec): "
                        + (jenkinsTaskParams.getTimeout()
                              - (System.currentTimeMillis() - jenkinsTaskParams.getStartTs()))
                            / 1000,
                    RUNNING));
          } else {
            executionStatus = ExecutionStatus.FAILED;
            jenkinsExecutionResponse.setErrorMessage(msg);
          }

          Build jenkinsBuild = waitForJobToStartExecution(jenkins, queueItem);
          jenkinsExecutionResponse.setBuildNumber(String.valueOf(jenkinsBuild.getNumber()));
          jenkinsExecutionResponse.setJobUrl(jenkinsBuild.getUrl());
        } catch (WingsException e) {
          logService.save(getAccountId(),
              constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                  jenkinsTaskParams.getAppId(), LogLevel.ERROR, msg + e.toString(), FAILURE));
          ExceptionLogger.logProcessedMessages(e, DELEGATE, logger);
          executionStatus = ExecutionStatus.FAILED;
          jenkinsExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
        } catch (Exception e) {
          logService.save(getAccountId(),
              constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                  jenkinsTaskParams.getAppId(), LogLevel.ERROR, msg + e.toString(), FAILURE));
          logger.error(msg, e);
          executionStatus = ExecutionStatus.FAILED;
          jenkinsExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
        }
        jenkinsExecutionResponse.setExecutionStatus(executionStatus);
        jenkinsExecutionResponse.setSubTaskType(JenkinsSubTaskType.START_TASK);
        jenkinsExecutionResponse.setActivityId(jenkinsTaskParams.getActivityId());
        return jenkinsExecutionResponse;

      case POLL_TASK:
        jenkinsExecutionResponse.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
        jenkinsExecutionResponse.setActivityId(jenkinsTaskParams.getActivityId());
        try {
          // Get jenkins build from queued URL
          logger.info(
              "The Jenkins queued url {} and retrieving build information", jenkinsTaskParams.getQueuedBuildUrl());
          Build jenkinsBuild = jenkins.getBuild(new QueueReference(jenkinsTaskParams.getQueuedBuildUrl()));
          if (jenkinsBuild == null) {
            logger.error(
                "Error occurred while retrieving the build {} status.  Job might have been deleted between poll intervals",
                jenkinsTaskParams.getQueuedBuildUrl());
            logService.save(getAccountId(),
                constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                    jenkinsTaskParams.getAppId(), LogLevel.INFO,
                    "Failed to get the build status " + jenkinsTaskParams.getQueuedBuildUrl(),
                    CommandExecutionStatus.FAILURE));
            jenkinsExecutionResponse.setErrorMessage(
                "Failed to get the build status. Job might have been deleted between poll intervals.");
            jenkinsExecutionResponse.setExecutionStatus(ExecutionStatus.FAILED);
            return jenkinsExecutionResponse;
          }

          jenkinsExecutionResponse.setBuildNumber(String.valueOf(jenkinsBuild.getNumber()));
          jenkinsExecutionResponse.setJobUrl(jenkinsBuild.getUrl());

          logService.save(getAccountId(),
              constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                  jenkinsTaskParams.getAppId(), LogLevel.INFO,
                  "Waiting for Jenkins task completion. Remaining time (sec): "
                      + (jenkinsTaskParams.getTimeout() - (System.currentTimeMillis() - jenkinsTaskParams.getStartTs()))
                          / 1000,
                  RUNNING));

          BuildWithDetails jenkinsBuildWithDetails = waitForJobExecutionToFinish(jenkinsBuild,
              jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(), jenkinsTaskParams.getAppId());
          jenkinsExecutionResponse.setJobUrl(jenkinsBuildWithDetails.getUrl());

          if (jenkinsTaskParams.isInjectEnvVars()) {
            logService.save(getAccountId(),
                constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                    jenkinsTaskParams.getAppId(), LogLevel.INFO, "Collecting environment variables for Jenkins task",
                    RUNNING));

            try {
              jenkinsExecutionResponse.setEnvVars(jenkins.getEnvVars(jenkinsBuildWithDetails.getUrl()));
            } catch (WingsException e) {
              logService.save(getAccountId(),
                  constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                      jenkinsTaskParams.getAppId(), LogLevel.ERROR,
                      (String) e.getParams().getOrDefault(
                          "message", "Failed to collect environment variables from Jenkins"),
                      FAILURE));
              throw e;
            }
          }

          logService.save(getAccountId(),
              constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(),
                  jenkinsTaskParams.getAppId(), LogLevel.INFO, "Jenkins task execution complete", SUCCESS));

          BuildResult buildResult = jenkinsBuildWithDetails.getResult();
          jenkinsExecutionResponse.setJenkinsResult(buildResult.toString());
          jenkinsExecutionResponse.setBuildNumber(String.valueOf(jenkinsBuildWithDetails.getNumber()));
          jenkinsExecutionResponse.setDescription(jenkinsBuildWithDetails.getDescription());
          jenkinsExecutionResponse.setBuildDisplayName(jenkinsBuildWithDetails.getDisplayName());
          jenkinsExecutionResponse.setBuildFullDisplayName(jenkinsBuildWithDetails.getFullDisplayName());

          try {
            jenkinsExecutionResponse.setJobParameters(jenkinsBuildWithDetails.getParameters());
          } catch (Exception e) { // cause buildWithDetails.getParameters() can throw NPE, unexpected exception
            logger.error("Error occurred while retrieving build parameters for build number {}",
                jenkinsBuildWithDetails.getNumber(), e);
            jenkinsExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
          }

          if (buildResult != BuildResult.SUCCESS
              && (buildResult != BuildResult.UNSTABLE || !jenkinsTaskParams.isUnstableSuccess())) {
            executionStatus = ExecutionStatus.FAILED;
          }
        } catch (WingsException e) {
          ExceptionLogger.logProcessedMessages(e, DELEGATE, logger);
          executionStatus = ExecutionStatus.FAILED;
          jenkinsExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
        } catch (Exception e) {
          logger.error("Error occurred while running Jenkins task", e);
          executionStatus = ExecutionStatus.FAILED;
          jenkinsExecutionResponse.setErrorMessage(ExceptionUtils.getMessage(e));
        }
        break;

      default:
        jenkinsExecutionResponse.setExecutionStatus(ExecutionStatus.ERROR);
        throw new InvalidRequestException("Unhandled case for Jenkins Sub task, neither start nor poll sub task.");
    }

    jenkinsExecutionResponse.setExecutionStatus(executionStatus);
    return jenkinsExecutionResponse;
  }

  private BuildWithDetails waitForJobExecutionToFinish(
      Build jenkinsBuild, String activityId, String unitName, String appId) throws IOException {
    BuildWithDetails jenkinsBuildWithDetails = null;
    AtomicInteger consoleLogsSent = new AtomicInteger();
    do {
      logger.info("Waiting for Job  {} to finish execution", jenkinsBuild.getUrl());
      sleep(Duration.ofSeconds(5));
      try {
        jenkinsBuildWithDetails = jenkinsBuild.details();
        saveConsoleLogs(jenkinsBuildWithDetails, consoleLogsSent, activityId, unitName, RUNNING, appId);
      } catch (IOException e) {
        if (e instanceof HttpResponseException) {
          if (((HttpResponseException) e).getStatusCode() == 404) {
            throw new HttpResponseException(((HttpResponseException) e).getStatusCode(),
                "Job [" + jenkinsBuild.getUrl()
                    + "] not found. Job might have been deleted from Jenkins Server between polling intervals");
          }
        }
        logger.error("Error occurred while waiting for Job {} to finish execution. Reasonn {}. Retrying.",
            jenkinsBuild.getUrl(), ExceptionUtils.getMessage(e));
      }

    } while (jenkinsBuildWithDetails == null || jenkinsBuildWithDetails.isBuilding());
    logger.info("Job {} execution completed. Status: {}", jenkinsBuildWithDetails.getNumber(),
        jenkinsBuildWithDetails.getResult());
    return jenkinsBuildWithDetails;
  }

  private Log constructLog(String activityId, String stateName, String appId, LogLevel logLevel, String logLine,
      CommandExecutionStatus commandExecutionStatus) {
    return aLog()
        .withActivityId(activityId)
        .withCommandUnitName(stateName)
        .withAppId(appId)
        .withLogLevel(logLevel)
        .withLogLine(logLine)
        .withExecutionResult(commandExecutionStatus)
        .build();
  }

  private void saveConsoleLogs(BuildWithDetails jenkinsBuildWithDetails, AtomicInteger consoleLogsAlreadySent,
      String activityId, String stateName, CommandExecutionStatus commandExecutionStatus, String appId)
      throws IOException {
    String consoleOutputText = jenkinsBuildWithDetails.getConsoleOutputText();
    if (isNotBlank(consoleOutputText)) {
      String[] consoleLines = consoleOutputText.split("\r\n");
      if (consoleLines.length > NUM_OF_LOGS_TO_KEEP) {
        Log log = aLog()
                      .withActivityId(activityId)
                      .withCommandUnitName(stateName)
                      .withAppId(appId)
                      .withLogLevel(LogLevel.INFO)
                      .withLogLine("-------------------------- truncating "
                          + (consoleLines.length - NUM_OF_LOGS_TO_KEEP) + " lines --------------------------")
                      .withExecutionResult(commandExecutionStatus)
                      .build();
        logService.save(getAccountId(), log);
      }
      for (int i = NUM_OF_LOGS_TO_KEEP > consoleLines.length ? consoleLogsAlreadySent.get()
                                                             : consoleLines.length - NUM_OF_LOGS_TO_KEEP;
           i < consoleLines.length; i++) {
        Log log = aLog()
                      .withActivityId(activityId)
                      .withCommandUnitName(stateName)
                      .withAppId(appId)
                      .withLogLevel(LogLevel.INFO)
                      .withLogLine(consoleLines[i])
                      .withExecutionResult(commandExecutionStatus)
                      .build();
        logService.save(getAccountId(), log);
        consoleLogsAlreadySent.incrementAndGet();
      }
    }
  }

  private Build waitForJobToStartExecution(Jenkins jenkins, QueueReference queueItem) {
    Build jenkinsBuild = null;
    do {
      logger.info("Waiting for job {} to start execution", queueItem);
      sleep(Duration.ofSeconds(1));
      try {
        jenkinsBuild = jenkins.getBuild(queueItem);
        if (jenkinsBuild != null) {
          logger.info("Job started and Build No {}", jenkinsBuild.getNumber());
        }
      } catch (IOException e) {
        logger.error("Error occurred while waiting for Job to start execution.", e);
        if (e instanceof HttpResponseException) {
          if (((HttpResponseException) e).getStatusCode() == 401) {
            throw new InvalidCredentialsException("Invalid Jenkins credentials", WingsException.USER);
          } else if (((HttpResponseException) e).getStatusCode() == 403) {
            throw new UnauthorizedException("User not authorized to access jenkins", WingsException.USER);
          }
          throw new GeneralException(e.getMessage());
        }
      }
    } while (jenkinsBuild == null);
    return jenkinsBuild;
  }
}
