package software.wings.delegatetasks.collect.artifacts;

import static software.wings.beans.BambooConfig.Builder.aBambooConfig;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.bamboo.BambooService;
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
public class BambooCollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private static final Logger logger = LoggerFactory.getLogger(BambooCollectionTask.class);

  @Inject private BambooService bambooService;
  @Inject private DelegateFileManager delegateFileManager;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public BambooCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<ListNotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (char[]) parameters[2], (String) parameters[3],
        (List<String>) parameters[4], (Map<String, String>) parameters[5]);
  }

  public ListNotifyResponseData run(String bambooUrl, String username, char[] password, String planKey,
      List<String> artifactPaths, Map<String, String> arguments) {
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      BambooConfig bambooConfig =
          aBambooConfig().withBambooUrl(bambooUrl).withUsername(username).withPassword(password).build();

      for (String artifactPath : artifactPaths) {
        Pair<String, InputStream> fileInfo =
            bambooService.downloadArtifact(bambooConfig, planKey, arguments.get(BUILD_NO), artifactPath);
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
