package software.wings.collect;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.beans.Event.Builder.anEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ApprovalNotification;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.NotificationService;
import software.wings.waitnotify.ListNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 12/20/16.
 */
public class ArtifactCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionCallback.class);

  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private EventEmitter eventEmitter;
  @Inject private NotificationService notificationService;

  private String appId;
  private String artifactId;

  public ArtifactCollectionCallback() {}

  public ArtifactCollectionCallback(String appId, String artifactId) {
    this.appId = appId;
    this.artifactId = artifactId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    ListNotifyResponseData responseData = (ListNotifyResponseData) response.values().iterator().next();
    Artifact artifact = artifactService.get(appId, artifactId);

    if (isNotEmpty(responseData.getData())) {
      artifactService.addArtifactFile(artifact.getUuid(), artifact.getAppId(), responseData.getData());
    } else {
      // TODO : error handling
      // throw new FileNotFoundException("unable to collect artifact ");
    }

    logger.info("Artifact collection completed - artifactId : {}", artifact.getUuid());
    artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.READY);

    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    if (artifactStream.isAutoApproveForProduction()) {
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.APPROVED);
    }
    artifactStreamService.triggerStreamActionPostArtifactCollectionAsync(artifact);
    eventEmitter.send(Channel.ARTIFACTS,
        anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());

    notificationService.sendNotificationAsync(ApprovalNotification.Builder.anApprovalNotification()
                                                  .withAppId(artifact.getAppId())
                                                  .withEntityId(artifact.getUuid())
                                                  .withEntityType(EntityType.ARTIFACT)
                                                  .withEntityName(artifact.getDisplayName())
                                                  .withArtifactStreamId(artifact.getArtifactStreamId())
                                                  .build());
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
