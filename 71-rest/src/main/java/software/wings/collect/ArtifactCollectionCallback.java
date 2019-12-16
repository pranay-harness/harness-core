package software.wings.collect;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.FAILED;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;

import com.google.inject.Inject;

import io.harness.delegate.beans.ResponseData;
import io.harness.waiter.ListNotifyResponseData;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.SettingsService;

import java.util.Map;

/**
 * Created by rishi on 12/20/16.
 */
@Slf4j
public class ArtifactCollectionCallback implements NotifyCallback {
  @Inject private ArtifactService artifactService;
  @Inject private EventEmitter eventEmitter;
  @Inject private NotificationService notificationService;
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;

  private String artifactId;
  private boolean acs;

  public ArtifactCollectionCallback() {}

  public ArtifactCollectionCallback(String artifactId) {
    this.artifactId = artifactId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    Artifact artifact = artifactService.get(artifactId);
    ListNotifyResponseData responseData = (ListNotifyResponseData) response.values().iterator().next();

    if (isEmpty(responseData.getData())) { // Error in Downloading artifact file
      logger.warn(
          "Artifact file collection failed for artifactId: [{}]. Marking content status as NOT_DOWNLOADED to retry next artifac check",
          artifactId);
      if (artifact == null) {
        logger.info("Artifact Id {} was deleted - nothing to do", artifactId);
        return;
      }
      artifactService.updateStatus(
          artifactId, artifact.getAccountId(), APPROVED, FAILED, "Failed to download artifact file");
    } else {
      logger.info("Artifact collection completed - artifactId : {}", artifactId);
      if (artifact == null) {
        logger.info("Artifact Id {} was deleted - nothing to do", artifactId);
        return;
      }
      artifactService.addArtifactFile(artifact.getUuid(), artifact.getAccountId(), responseData.getData());
      artifactService.updateStatus(artifactId, artifact.getAccountId(), APPROVED, DOWNLOADED, "");
      String accountId = null;
      ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
      if (artifactStream != null) {
        SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
        if (settingAttribute != null) {
          accountId = settingAttribute.getAccountId();
        }
      }
      notificationService.sendNotificationAsync(anApprovalNotification()
                                                    .withAppId(artifact.fetchAppId())
                                                    .withEntityId(artifact.getUuid())
                                                    .withEntityType(EntityType.ARTIFACT)
                                                    .withEntityName(artifact.getDisplayName())
                                                    .withArtifactStreamId(artifact.getArtifactStreamId())
                                                    .withAccountId(accountId)
                                                    .build());
    }

    // NOTE: artifact != null as this condition was checked above
    if (!GLOBAL_APP_ID.equals(artifact.fetchAppId())) {
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifactId).withAppId(artifact.fetchAppId()).build());
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info("Error occurred while collecting content of artifact id {}", artifactId);
    Artifact artifact = artifactService.get(artifactId);
    if (artifact == null) {
      logger.info("Artifact Id {} was deleted - nothing to do", artifactId);
      return;
    }
    artifactService.updateStatus(artifact.getUuid(), artifact.getAccountId(), APPROVED, FAILED);
    if (!GLOBAL_APP_ID.equals(artifact.fetchAppId())) {
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.fetchAppId()).build());
    }
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
}
