package software.wings.collect;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.ERROR;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.READY;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ApprovalNotification.ApprovalStage;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.TriggerService;
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
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private EventEmitter eventEmitter;
  @Inject private NotificationService notificationService;
  @Inject private TriggerService triggerService;

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
      logger.error("Artifact file collection failed for artifactId: [{}], appId: [{}]", artifactId, appId);
      artifactService.updateStatus(artifactId, appId, FAILED, "Failed to download artifact file");
    } else {
      Artifact artifact = artifactService.get(appId, artifactId);
      logger.info("Artifact collection completed - artifactId : {}", artifact.getUuid());
      artifactService.addArtifactFile(artifact.getUuid(), artifact.getAppId(), responseData.getData());
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), READY);

      ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

      if (artifactStream.isAutoApproveForProduction()) {
        artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), APPROVED);
      }
      artifact = artifactService.get(appId, artifactId);
      triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
      notificationService.sendNotificationAsync(
          anApprovalNotification()
              .withAppId(artifact.getAppId())
              .withStage(artifactStream.isAutoApproveForProduction() ? ApprovalStage.APPROVED : ApprovalStage.PENDING)
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
    Artifact artifact = artifactService.get(appId, artifactId);
    artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), ERROR);
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
