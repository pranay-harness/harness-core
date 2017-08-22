package software.wings.delegatetasks.collect.artifacts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.utils.Misc;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by srinivas on 4/4/17.
 */
public class ArtifactoryCollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactoryCollectionTask.class);

  @Inject private ArtifactoryService artifactoryService;

  @Inject private DelegateFileManager delegateFileManager;

  public ArtifactoryCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<ListNotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      return run((String) parameters[0], (String) parameters[1], (char[]) parameters[2], (String) parameters[3],
          (String) parameters[4], (List<String>) parameters[5], (String) parameters[6],
          (Map<String, String>) parameters[7]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(String artifactoryUrl, String username, char[] password, String repoType,
      String groupId, List<String> artifactPaths, String artifactPattern, Map<String, String> arguments) {
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();
    /* try {

       *//*ArtifactoryConfig artifactoryConfig = anAritfactoryConfig().withNexusUrl(artifactoryUrl).withUsername(username)
          .withPassword(password).build();
      for (String artifactPath : artifactPaths) {
        logger.info ("Collecting artifact {}  from Nexus server {}", artifactPath, artifactoryUrl);
        Pair<String, InputStream> fileInfo =
            nexusService.downloadArtifact(nexusConfig, repoType, groupId,artifactPath);
        if (fileInfo == null) {
          throw new FileNotFoundException(
              "Unable to get artifact from nexus for path " + artifactPath);
        }
        in = fileInfo.getValue();
        logger.info ("Uploading the file {} " , fileInfo.getKey ());
        DelegateFile delegateFile = aDelegateFile().withFileName(fileInfo.getKey())
            .withDelegateId(getDelegateId()).withTaskId(getTaskId()).withAccountId(getAccountId())
            .build(); //TODO: more about delegate and task info
        DelegateFile fileRes = delegateFileManager.upload(delegateFile, in);
        logger.info ("Uploaded the file {} " , fileInfo.getKey ());
        ArtifactFile artifactFile = new ArtifactFile();
        artifactFile.setFileUuid(fileRes.getFileId());
        artifactFile.setName(fileInfo.getKey());
        res.addData(artifactFile);*//*
      }
    } catch (Exception e) {
      logger.warn("Exception", e);
      //TODO: better error handling

//      if (e instanceof WingsException)
//        WingsException ex = (WingsException) e;
//        errorMessage = Joiner.on(",").join(ex.getResponseMessageList().stream()
//            .map(responseMessage -> ResponseCodeCache.getInstance().getResponseMessage(responseMessage.getCode(), ex.getParams()).getMessage())
//            .collect(toList()));
//      } else {
//        errorMessage = e.getMessage();
//      }
//      executionStatus = executionStatus.FAILED;
//      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    } finally {
      IOUtils.closeQuietly(in);
    }*/
    return res;
  }
}
