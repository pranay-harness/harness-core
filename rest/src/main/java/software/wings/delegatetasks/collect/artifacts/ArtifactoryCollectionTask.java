package software.wings.delegatetasks.collect.artifacts;

import static software.wings.beans.config.ArtifactoryConfig.Builder.anArtifactoryConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.waitnotify.ListNotifyResponseData;

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
      String groupId, List<String> artifactPaths, String artifactPattern, Map<String, String> metadata) {
    try {
      ArtifactoryConfig artifactoryConfig = anArtifactoryConfig()
                                                .withArtifactoryUrl(artifactoryUrl)
                                                .withUsername(username)
                                                .withPassword(password)
                                                .build();
      return artifactoryService.downloadArtifacts(artifactoryConfig, repoType, groupId, artifactPaths, artifactPattern,
          metadata, getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      logger.warn(
          "Exception occurred while collecting artifact for artifact server {}  " + e.getMessage(), artifactoryUrl, e);
    }
    return new ListNotifyResponseData();
  }
}
