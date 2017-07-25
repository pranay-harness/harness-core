package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.MapUtils.isEmpty;

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
import software.wings.utils.ExpressionEvaluator;
import software.wings.utils.Misc;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rishi on 12/14/16.
 */
public class JenkinsTask extends AbstractDelegateRunnableTask<JenkinsExecutionResponse> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private ExpressionEvaluator evaluator;
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
    String errorMessage = null;
    try {
      Jenkins jenkins = jenkinsFactory.create(jenkinsUrl, username, password);

      QueueReference queueItem = jenkins.trigger(finalJobName, evaluatedParameters);

      Build jenkinsBuild;
      while ((jenkinsBuild = jenkins.getBuild(queueItem)) == null) {
        Misc.sleepWithRuntimeException(1000);
      }
      BuildWithDetails jenkinsBuildWithDetails;
      while ((jenkinsBuildWithDetails = jenkinsBuild.details()).isBuilding()) {
        Misc.sleepWithRuntimeException((int) (Math.max(
            5000, jenkinsBuildWithDetails.getDuration() - jenkinsBuildWithDetails.getEstimatedDuration())));
      }
      jenkinsExecutionResponse.setJobUrl(jenkinsBuild.getUrl());

      BuildResult buildResult = jenkinsBuildWithDetails.getResult();
      jenkinsExecutionResponse.setJenkinsResult(buildResult.toString());

      if (buildResult == BuildResult.SUCCESS || buildResult == BuildResult.UNSTABLE) {
        if (!isEmpty(evaluatedFilePathsForAssertion)) {
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
      Misc.warn(logger, "Exception: " + e.getMessage(), e);
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
      executionStatus = executionStatus.FAILED;
      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    }
    jenkinsExecutionResponse.setExecutionStatus(executionStatus);
    return jenkinsExecutionResponse;
  }
}
