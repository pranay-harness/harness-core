package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.service.impl.LogServiceImpl.NUM_OF_LOGS_TO_KEEP;

import com.google.inject.Inject;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.JenkinsTaskParams;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.service.impl.jenkins.JenkinsUtil;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rishi on 12/14/16.
 */
public class JenkinsTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(JenkinsTask.class);

  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private JenkinsUtil jenkinsUtil;

  public JenkinsTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public JenkinsExecutionResponse run(Object[] parameters) {
    return run((JenkinsTaskParams) parameters[0]);
  }

  public JenkinsExecutionResponse run(JenkinsTaskParams jenkinsTaskParams) {
    JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    try {
      JenkinsConfig jenkinsConfig = jenkinsTaskParams.getJenkinsConfig();
      encryptionService.decrypt(jenkinsConfig, jenkinsTaskParams.getEncryptedDataDetails());
      Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);

      logger.info("In JenkinsTask Triggering Job {}", jenkinsTaskParams.getJobName());
      QueueReference queueItem = jenkins.trigger(jenkinsTaskParams.getJobName(), jenkinsTaskParams.getParameters());
      logger.info("Triggered Job success and queue item Url part {}",
          queueItem == null ? null : queueItem.getQueueItemUrlPart());
      Build jenkinsBuild = waitForJobToStartExecution(jenkins, queueItem);
      jenkinsExecutionResponse.setBuildNumber(String.valueOf(jenkinsBuild.getNumber()));
      jenkinsExecutionResponse.setJobUrl(jenkinsBuild.getUrl());

      BuildWithDetails jenkinsBuildWithDetails =
          waitForJobExecutionToFinish(jenkinsBuild, jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName());

      jenkinsExecutionResponse.setJobUrl(jenkinsBuildWithDetails.getUrl());

      BuildResult buildResult = jenkinsBuildWithDetails.getResult();
      jenkinsExecutionResponse.setJenkinsResult(buildResult.toString());
      jenkinsExecutionResponse.setBuildNumber(String.valueOf(jenkinsBuildWithDetails.getNumber()));
      jenkinsExecutionResponse.setDescription(jenkinsBuildWithDetails.getDescription());
      jenkinsExecutionResponse.setBuildDisplayName(jenkinsBuildWithDetails.getDisplayName());
      jenkinsExecutionResponse.setBuildFullDisplayName(jenkinsBuildWithDetails.getFullDisplayName());

      try {
        jenkinsExecutionResponse.setJobParameters(jenkinsBuildWithDetails.getParameters());
      } catch (Exception e) { // cause buildWithDetails.getParameters() can throw NPE
        // unexpected exception
        logger.error("Error occurred while retrieving build parameters for build number {}",
            jenkinsBuildWithDetails.getNumber(), e);
        jenkinsExecutionResponse.setErrorMessage(Misc.getMessage(e));
      }

      if (buildResult != BuildResult.SUCCESS
          && (buildResult != BuildResult.UNSTABLE || !jenkinsTaskParams.isUnstableSuccess())) {
        executionStatus = ExecutionStatus.FAILED;
      }
    } catch (Exception e) {
      logger.error("Error occurred while running Jenkins task", e);
      executionStatus = ExecutionStatus.FAILED;
      jenkinsExecutionResponse.setErrorMessage(Misc.getMessage(e));
    }
    jenkinsExecutionResponse.setExecutionStatus(executionStatus);
    return jenkinsExecutionResponse;
  }

  private BuildWithDetails waitForJobExecutionToFinish(Build jenkinsBuild, String activityId, String unitName) {
    BuildWithDetails jenkinsBuildWithDetails = null;
    AtomicInteger consoleLogsSent = new AtomicInteger();
    do {
      logger.info("Waiting for Job  {} to finish execution", jenkinsBuild.getUrl());
      sleep(Duration.ofSeconds(5));
      try {
        jenkinsBuildWithDetails = jenkinsBuild.details();
        saveConsoleLogs(jenkinsBuildWithDetails, consoleLogsSent, activityId, unitName, RUNNING);
      } catch (IOException e) {
        logger.error("Error occurred while waiting for Job to start execution. Retrying. {}", Misc.getMessage(e));
      }

    } while (jenkinsBuildWithDetails == null || jenkinsBuildWithDetails.isBuilding());
    logger.info("Job {} execution completed. Status: {}", jenkinsBuildWithDetails.getNumber(),
        jenkinsBuildWithDetails.getResult());
    return jenkinsBuildWithDetails;
  }

  private void saveConsoleLogs(BuildWithDetails jenkinsBuildWithDetails, AtomicInteger consoleLogsAlreadySent,
      String activityId, String stateName, CommandExecutionStatus commandExecutionStatus) throws IOException {
    String consoleOutputText = jenkinsBuildWithDetails.getConsoleOutputText();
    if (isNotBlank(consoleOutputText)) {
      String[] consoleLines = consoleOutputText.split("\r\n");
      if (consoleLines.length > NUM_OF_LOGS_TO_KEEP) {
        Log log = aLog()
                      .withActivityId(activityId)
                      .withCommandUnitName(stateName)
                      .withAppId(getAppId())
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
                      .withAppId(getAppId())
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
            throw new WingsException("Invalid Jenkins credentials");
          } else if (((HttpResponseException) e).getStatusCode() == 403) {
            throw new WingsException("User not authorized to access jenkins");
          }
          throw new WingsException(e.getMessage());
        }
      }
    } while (jenkinsBuild == null);
    return jenkinsBuild;
  }
}
