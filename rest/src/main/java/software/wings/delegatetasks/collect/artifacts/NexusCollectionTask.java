package software.wings.delegatetasks.collect.artifacts;

import static software.wings.beans.config.NexusConfig.Builder.aNexusConfig;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.nexus.NexusService;
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
 * Created by srinivas on 4/4/17.
 */
public class NexusCollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private static final Logger logger = LoggerFactory.getLogger(NexusCollectionTask.class);

  @Inject private NexusService nexusService;

  @Inject private DelegateFileManager delegateFileManager;

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public NexusCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<ListNotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      return run((String) parameters[0], (String) parameters[1], (char[]) parameters[2], (String) parameters[3],
          (String) parameters[4], (List<String>) parameters[5], (Map<String, String>) parameters[6]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(String nexusUrl, String username, char[] password, String repoType, String groupId,
      List<String> artifactPaths, Map<String, String> arguments) {
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();
    try {
      NexusConfig nexusConfig =
          aNexusConfig().withNexusUrl(nexusUrl).withUsername(username).withPassword(password).build();
      for (String artifactPath : artifactPaths) {
        logger.info("Collecting artifact {}  from Nexus server {}", artifactPath, nexusUrl);
        Pair<String, InputStream> fileInfo =
            nexusService.downloadArtifact(nexusConfig, repoType, groupId, artifactPath);
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
