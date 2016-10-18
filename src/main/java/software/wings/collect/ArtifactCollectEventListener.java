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
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.NotificationService;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 5/11/16.
 *
 * @see CollectEvent
 */
@Singleton
public class ArtifactCollectEventListener extends AbstractQueueListener<CollectEvent> {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectEventListener.class);

  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private NotificationService notificationService;

  @Inject private Map<String, ArtifactCollectorService> artifactCollectorServiceMap;
  @Inject private EventEmitter eventEmitter;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(CollectEvent message) throws Exception {
    Artifact artifact = message.getArtifact();
    try {
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.RUNNING);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());

      ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId(), artifact.getAppId());
      ArtifactCollectorService artifactCollectorService =
          artifactCollectorServiceMap.get(artifactStream.getSourceType().name());
      List<ArtifactFile> artifactFiles = artifactCollectorService.collect(artifactStream, artifact.getMetadata());

      if (isNotEmpty(artifactFiles)) {
        artifactService.addArtifactFile(artifact.getUuid(), artifact.getAppId(), artifactFiles);
      } else {
        throw new FileNotFoundException("unable to collect artifact ");
      }
      logger.info("Artifact collection completed - artifactId : {}", artifact.getUuid());
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.READY);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());

      notificationService.sendNotificationAsync(ApprovalNotification.Builder.anApprovalNotification()
                                                    .withAppId(artifact.getAppId())
                                                    .withEntityId(artifact.getUuid())
                                                    .withEntityType(EntityType.ARTIFACT)
                                                    .withEntityName(artifact.getDisplayName())
                                                    .withArtifactStreamId(artifact.getArtifactStreamId())
                                                    .build());
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.FAILED);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());
    }
  }
}
