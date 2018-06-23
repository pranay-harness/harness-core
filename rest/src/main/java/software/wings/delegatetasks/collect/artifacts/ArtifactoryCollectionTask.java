package software.wings.delegatetasks.collect.artifacts;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;
import software.wings.waitnotify.ListNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by srinivas on 4/4/17.
 */
public class ArtifactoryCollectionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactoryCollectionTask.class);

  @Inject private ArtifactoryService artifactoryService;

  public ArtifactoryCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      return run((ArtifactoryConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1], (String) parameters[2],
          (String) parameters[3], (List<String>) parameters[4], (String) parameters[5],
          (Map<String, String>) parameters[6]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String repoType, String groupId, List<String> artifactPaths, String artifactPattern,
      Map<String, String> metadata) {
    try {
      return artifactoryService.downloadArtifacts(artifactoryConfig, encryptedDataDetails, repoType, groupId,
          artifactPaths, artifactPattern, metadata, getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      logger.warn("Exception occurred while collecting artifact for artifact server {} : {}",
          artifactoryConfig.getArtifactoryUrl(), Misc.getMessage(e), e);
    }
    return new ListNotifyResponseData();
  }
}
