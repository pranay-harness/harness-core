package software.wings.delegatetasks.collect.artifacts;

import static software.wings.common.Constants.BUILD_NO;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.utils.Misc;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rishi on 12/14/16.
 */
public class JenkinsCollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private static final Logger logger = LoggerFactory.getLogger(JenkinsCollectionTask.class);

  @Inject private JenkinsFactory jenkinsFactory;
  @Inject private DelegateFileManager delegateFileManager;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public JenkinsCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<ListNotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (char[]) parameters[2], (String) parameters[3],
        (List<String>) parameters[4], (Map<String, String>) parameters[5]);
  }

  public ListNotifyResponseData run(String jenkinsUrl, String username, char[] password, String jobName,
      List<String> artifactPaths, Map<String, String> arguments) {
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      Jenkins jenkins = jenkinsFactory.create(jenkinsUrl, username, password);

      for (String artifactPath : artifactPaths) {
        Pair<String, InputStream> fileInfo = jenkins.downloadArtifact(jobName, arguments.get(BUILD_NO), artifactPath);
        artifactCollectionTaskHelper.addDataToResponse(
            fileInfo, artifactPath, res, getDelegateId(), getTaskId(), getAccountId());
      }
    } catch (Exception e) {
      logger.warn("Exception: " + e.getMessage(), e);
      // TODO: better error handling

      //      if (e instanceof WingsException)
      //        WingsException ex = (WingsException) e;
      //        errorMessage = Joiner.on(",").join(ex.getResponseMessageList().stream()
      //            .map(responseMessage ->
      //            ResponseCodeCache.getInstance().getResponseMessage(responseMessage.getCode(),
      //            ex.getParams()).getMessage()) .collect(toList()));
      //      } else {
      //        errorMessage = e.getMessage();
      //      }
      //      executionStatus = executionStatus.FAILED;
      //      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    } finally {
      IOUtils.closeQuietly(in);
    }

    return res;
  }
}
