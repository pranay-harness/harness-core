package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.service.impl.LogServiceImpl.NUM_OF_LOGS_TO_KEEP;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.JenkinsTaskParams;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;
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

  @Inject private JenkinsFactory jenkinsFactory;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;

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
    String errorMessage;
    try {
      JenkinsConfig jenkinsConfig = jenkinsTaskParams.getJenkinsConfig();
      encryptionService.decrypt(jenkinsConfig, jenkinsTaskParams.getEncryptedDataDetails());
      Jenkins jenkins = jenkinsFactory.create(
          jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());

      QueueReference queueItem = jenkins.trigger(jenkinsTaskParams.getJobName(), jenkinsTaskParams.getParameters());

      Build jenkinsBuild = waitForJobToStartExecution(jenkins, queueItem);
      BuildWithDetails jenkinsBuildWithDetails =
          waitForJobExecutionToFinish(jenkinsBuild, jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName());

      jenkinsExecutionResponse.setJobUrl(jenkinsBuildWithDetails.getUrl());

      BuildResult buildResult = jenkinsBuildWithDetails.getResult();
      jenkinsExecutionResponse.setJenkinsResult(buildResult.toString());
      jenkinsExecutionResponse.setBuildNumber(String.valueOf(jenkinsBuildWithDetails.getNumber()));
      jenkinsExecutionResponse.setDescription(jenkinsBuildWithDetails.getDescription());
      try {
        jenkinsExecutionResponse.setJobParameters(jenkinsBuildWithDetails.getParameters());
      } catch (Exception e) { // cause buildWithDetails.getParameters() can throw NPE
        // unexpected exception
        logger.warn("Error occurred while retrieving build parameters for build number {} ",
            jenkinsBuildWithDetails.getNumber(), e.getMessage());
      }

      if (buildResult == BuildResult.SUCCESS
          || (buildResult == BuildResult.UNSTABLE && jenkinsTaskParams.isUnstableSuccess())) {
        if (isNotEmpty(jenkinsTaskParams.getFilePathsForAssertion())) {
          //          for (Entry<String, String> entry : evaluatedFilePathsForAssertion.entrySet()) {
          //            String filePathForAssertion = entry.getKey();
          //            String assertion = entry.getValue();
          //
          //            Pattern pattern = Pattern.compile(filePathForAssertion.replace(".", "\\.").replace("?",
          //            ".?").replace("*", ".*?")); Optional<Artifact> artifactOptional =
          //                jenkinsBuildWithDetails.getArtifacts().stream().filter(artifact ->
          //                pattern.matcher(artifact.getRelativePath()).matches()).findFirst();
          //            if (artifactOptional.isPresent()) {
          //              String data = CharStreams.toString(new
          //              InputStreamReader(jenkinsBuildWithDetails.downloadArtifact(artifactOptional.get())));
          //              FilePathAssertionEntry filePathAssertionEntry = new
          //              FilePathAssertionEntry(artifactOptional.get().getRelativePath(), assertion, data);
          //              filePathAssertionEntry
          //                  .setStatus(Boolean.TRUE.equals(evaluator.evaluateExpression(assertion,
          //                  filePathAssertionEntry)) ? Status.SUCCESS : Status.FAILED);
          //              jenkinsExecutionResponse.getFilePathAssertionMap().add(filePathAssertionEntry);
          //            } else {
          //              executionStatus = ExecutionStatus.FAILED;
          //              jenkinsExecutionResponse.getFilePathAssertionMap().add(new
          //              FilePathAssertionEntry(filePathForAssertion, assertion, Status.NOT_FOUND));
          //            }
          //          }
        }
      } else {
        executionStatus = ExecutionStatus.FAILED;
      }
    } catch (Exception e) {
      logger.warn("Exception: " + e.getMessage(), e);
      if (e instanceof WingsException) {
        WingsException ex = (WingsException) e;
        errorMessage = Joiner.on(",").join(ex.getResponseMessageList(ReportTarget.USER));
      } else {
        errorMessage = e.getMessage();
      }
      executionStatus = ExecutionStatus.FAILED;
      jenkinsExecutionResponse.setErrorMessage(errorMessage);
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
        saveConsoleLogs(jenkinsBuildWithDetails, consoleLogsSent, activityId, unitName);
      } catch (IOException ex) {
        logger.warn("Jenkins server unreachable {}", ex.getMessage());
      }
    } while (jenkinsBuildWithDetails == null || jenkinsBuildWithDetails.isBuilding());
    logger.info("Job {} execution completed. Status:", jenkinsBuildWithDetails.getNumber(),
        jenkinsBuildWithDetails.getResult());
    return jenkinsBuildWithDetails;
  }

  private void saveConsoleLogs(BuildWithDetails jenkinsBuildWithDetails, AtomicInteger consoleLogsAlreadySent,
      String activityId, String stateName) throws IOException {
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
                      .withExecutionResult(CommandExecutionStatus.RUNNING)
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
                      .withExecutionResult(CommandExecutionStatus.RUNNING)
                      .build();
        logService.save(getAccountId(), log);
        consoleLogsAlreadySent.incrementAndGet();
      }
    }
  }

  private Build waitForJobToStartExecution(Jenkins jenkins, QueueReference queueItem) {
    Build jenkinsBuild = null;
    do {
      sleep(Duration.ofSeconds(1));
      try {
        jenkinsBuild = jenkins.getBuild(queueItem);
      } catch (IOException ex) {
        logger.warn("Jenkins server unreachable {}", ex.getMessage());
      }
    } while (jenkinsBuild == null);
    return jenkinsBuild;
  }
}
