package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.MapUtils.isNotEmpty;

import com.google.common.base.Joiner;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rishi on 12/14/16.
 */
public class JenkinsTask extends AbstractDelegateRunnableTask<JenkinsExecutionResponse> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private JenkinsFactory jenkinsFactory;

  public JenkinsTask(String delegateId, DelegateTask delegateTask, Consumer<JenkinsExecutionResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public JenkinsExecutionResponse run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (char[]) parameters[2], (String) parameters[3],
        (Map<String, String>) parameters[4], (Map<String, String>) parameters[5]);
  }

  public JenkinsExecutionResponse run(String jenkinsUrl, String username, char[] password, String finalJobName,
      Map<String, String> evaluatedParameters, Map<String, String> evaluatedFilePathsForAssertion) {
    JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage;
    try {
      Jenkins jenkins = jenkinsFactory.create(jenkinsUrl, username, password);

      QueueReference queueItem = jenkins.trigger(finalJobName, evaluatedParameters);

      Build jenkinsBuild = waitForJobToStartExecution(jenkins, queueItem);
      BuildWithDetails jenkinsBuildWithDetails = waitForJobExecutionToFinish(jenkinsBuild);

      jenkinsExecutionResponse.setJobUrl(jenkinsBuild.getUrl());

      BuildResult buildResult = jenkinsBuildWithDetails.getResult();
      jenkinsExecutionResponse.setJenkinsResult(buildResult.toString());

      if (buildResult == BuildResult.SUCCESS || buildResult == BuildResult.UNSTABLE) {
        if (isNotEmpty(evaluatedFilePathsForAssertion)) {
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
        errorMessage = Joiner.on(",").join(ex.getResponseMessageList()
                                               .stream()
                                               .map(responseMessage
                                                   -> ResponseCodeCache.getInstance()
                                                          .getResponseMessage(responseMessage.getCode(), ex.getParams())
                                                          .getMessage())
                                               .collect(toList()));
      } else {
        errorMessage = e.getMessage();
      }
      executionStatus = ExecutionStatus.FAILED;
      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    }
    jenkinsExecutionResponse.setExecutionStatus(executionStatus);
    return jenkinsExecutionResponse;
  }

  private BuildWithDetails waitForJobExecutionToFinish(Build jenkinsBuild) throws IOException {
    BuildWithDetails jenkinsBuildWithDetails = null;
    do {
      logger.info("Waiting for Job  {} to finish execution", jenkinsBuild.getUrl());
      Misc.sleepWithRuntimeException(5000);
      try {
        jenkinsBuildWithDetails = jenkinsBuild.details();
      } catch (IOException ex) {
        logger.warn("Jenkins server unreachable {}", ex.getMessage());
      }
    } while (jenkinsBuildWithDetails == null || jenkinsBuildWithDetails.isBuilding());
    logger.info("Job {} execution completed. Status:", jenkinsBuildWithDetails.getNumber(),
        jenkinsBuildWithDetails.getResult());
    return jenkinsBuildWithDetails;
  }

  private Build waitForJobToStartExecution(Jenkins jenkins, QueueReference queueItem) throws IOException {
    Build jenkinsBuild = null;
    do {
      logger.info("Waiting for Job  {} to start execution", queueItem.getQueueItemUrlPart());
      Misc.sleepWithRuntimeException(1000);
      try {
        jenkinsBuild = jenkins.getBuild(queueItem);
      } catch (IOException ex) {
        logger.warn("Jenkins server unreachable {}", ex.getMessage());
      }
    } while (jenkinsBuild == null);
    logger.info("Job  execution for job {} started. Url  is {} and build number is {} ",
        queueItem.getQueueItemUrlPart(), jenkinsBuild.getUrl(), jenkinsBuild.getNumber());
    return jenkinsBuild;
  }
}
