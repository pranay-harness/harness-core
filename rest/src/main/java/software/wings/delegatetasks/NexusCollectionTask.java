package software.wings.delegatetasks;

import static software.wings.beans.config.NexusConfig.Builder.aNexusConfig;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.waitnotify.ListNotifyResponseData;

/**
 * Created by srinivas on 4/4/17.
 */
public class NexusCollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private static final Logger logger = LoggerFactory.getLogger(NexusCollectionTask.class);

  @Inject private NexusService nexusService;

  @Inject private DelegateFileManager delegateFileManager;

  public NexusCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<ListNotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (String) parameters[2], (String) parameters[3],
        (String) parameters[4], (List<String>) parameters[5], (Map<String, String>) parameters[6]);
  }

  public ListNotifyResponseData run(String nexusUrl, String username, String password, String repoType, String groupId,
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
        if (fileInfo == null) {
          throw new FileNotFoundException("Unable to get artifact from nexus for path " + artifactPath);
        }
        in = fileInfo.getValue();
        logger.info("Uploading the file {} ", fileInfo.getKey());
        DelegateFile delegateFile = aDelegateFile()
                                        .withFileName(fileInfo.getKey())
                                        .withDelegateId(getDelegateId())
                                        .withTaskId(getTaskId())
                                        .withAccountId(getAccountId())
                                        .build(); // TODO: more about delegate and task info
        DelegateFile fileRes = delegateFileManager.upload(delegateFile, in);
        logger.info("Uploaded the file {} ", fileInfo.getKey());
        ArtifactFile artifactFile = new ArtifactFile();
        artifactFile.setFileUuid(fileRes.getFileId());
        artifactFile.setName(fileInfo.getKey());
        res.addData(artifactFile);
      }
    } catch (Exception e) {
      logger.warn("Exception: ", e);
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
