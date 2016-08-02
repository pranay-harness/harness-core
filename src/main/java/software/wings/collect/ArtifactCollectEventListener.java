package software.wings.collect;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.EntityType.ARTIFACT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.beans.ArtifactSource;
import software.wings.beans.Release;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;
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
  @Inject private NotificationService notificationService;

  @Inject private Map<String, ArtifactCollectorService> artifactCollectorServiceMap;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(CollectEvent message) throws Exception {
    Artifact artifact = message.getArtifact();
    try {
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.RUNNING);
      Release release = message.getArtifact().getRelease();

      ArtifactSource artifactSource = release.get(artifact.getArtifactSourceName());
      ArtifactCollectorService artifactCollectorService =
          artifactCollectorServiceMap.get(artifactSource.getSourceType().name());
      List<ArtifactFile> artifactFiles = artifactCollectorService.collect(artifactSource, artifact.getMetadata());

      if (isNotEmpty(artifactFiles)) {
        artifactService.addArtifactFile(artifact.getUuid(), artifact.getAppId(), artifactFiles);
      } else {
        throw new FileNotFoundException("unable to collect artifact ");
      }

      logger.info("Artifact collection completed - artifactId : {}", artifact.getUuid());
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.READY);

      notificationService.sendNotificationAsync(anApprovalNotification()
                                                    .withAppId(artifact.getAppId())
                                                    .withEntityId(artifact.getUuid())
                                                    .withEntityType(ARTIFACT)
                                                    .withEntityName(artifact.getDisplayName())
                                                    .withReleaseId(artifact.getRelease().getUuid())
                                                    .build());
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.FAILED);
    }
  }
}
