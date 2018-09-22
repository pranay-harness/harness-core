package software.wings.collect;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.FAILED;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.ERROR;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.NotificationService;
import software.wings.waitnotify.ListNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rishi on 12/20/16.
 */
public class ArtifactCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionCallback.class);

  @Inject private ArtifactService artifactService;
  @Inject private EventEmitter eventEmitter;
  @Inject private NotificationService notificationService;

  private String appId;
  private String artifactId;
  private boolean acs;

  public ArtifactCollectionCallback() {}

  public ArtifactCollectionCallback(String appId, String artifactId) {
    this.appId = appId;
    this.artifactId = artifactId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    ListNotifyResponseData responseData = (ListNotifyResponseData) response.values().iterator().next();

    if (isEmpty(responseData.getData())) { // Error in Downloading artifact file
      logger.warn(
          "Artifact file collection failed for artifactId: [{}], appId: [{}]. Marking content status as NOT_DOWNLOADED to retry next artifac check",
          artifactId, appId);
      artifactService.updateStatus(artifactId, appId, APPROVED, FAILED, "Failed to download artifact file");
    } else {
      logger.info("Artifact collection completed - artifactId : {}", artifactId);
      Artifact artifact = artifactService.get(appId, artifactId);
      if (artifact == null) {
        logger.info("Artifact Id {} was deleted - nothing to do", artifactId);
        return;
      }
      artifactService.addArtifactFile(artifact.getUuid(), artifact.getAppId(), responseData.getData());
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), APPROVED, DOWNLOADED);

      notificationService.sendNotificationAsync(anApprovalNotification()
                                                    .withAppId(artifact.getAppId())
                                                    .withEntityId(artifact.getUuid())
                                                    .withEntityType(EntityType.ARTIFACT)
                                                    .withEntityName(artifact.getDisplayName())
                                                    .withArtifactStreamId(artifact.getArtifactStreamId())
                                                    .build());
    }
    eventEmitter.send(Channel.ARTIFACTS, anEvent().withType(Type.UPDATE).withUuid(artifactId).withAppId(appId).build());
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.info("Error occurred while collecting content of artifact id {}", artifactId);
    Artifact artifact = artifactService.get(appId, artifactId);
    if (artifact == null) {
      logger.info("Artifact Id {} was deleted - nothing to do", artifactId);
      return;
    }
    artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), ERROR, FAILED);
    eventEmitter.send(Channel.ARTIFACTS,
        anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
}
