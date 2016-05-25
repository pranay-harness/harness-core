package software.wings.collect;

import static com.google.inject.name.Names.named;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import com.google.inject.Injector;
import com.google.inject.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.beans.ArtifactSource;
import software.wings.beans.ArtifactSourceMetadata;
import software.wings.beans.Release;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
@Singleton
public class ArtifactCollectEventListener extends AbstractQueueListener<CollectEvent> {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectEventListener.class);

  @Inject private ArtifactService artifactService;

  @Inject private Map<String, ArtifactCollectorService> artifactCollectorServiceMap;

  @Override
  protected void onMessage(CollectEvent message) throws Exception {
    Artifact artifact = message.getArtifact();
    try {
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.RUNNING);
      Release release = message.getArtifact().getRelease();
      for (ArtifactSourceMetadata artifactSourceMetadata : message.getArtifact().getArtifactSourceMetadatas()) {
        collectArtifactSource(artifact, release, artifactSourceMetadata);
      }
      logger.info("Artifact collection completed - artifactId : {}", artifact.getUuid());
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.READY);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.FAILED);
    }
  }

  private void collectArtifactSource(Artifact artifact, Release release, ArtifactSourceMetadata artifactSourceMetadata)
      throws FileNotFoundException {
    String artifactSourceName = artifactSourceMetadata.getArtifactSourceName();
    ArtifactSource artifactSource = release.get(artifactSourceName);
    ArtifactCollectorService artifactCollectorService =
        artifactCollectorServiceMap.get(artifactSource.getSourceType().name());
    List<ArtifactFile> artifactFiles =
        artifactCollectorService.collect(artifactSource, artifactSourceMetadata.getMetadata());

    if (isNotEmpty(artifactFiles)) {
      artifactService.addArtifactFile(artifact.getUuid(), artifact.getAppId(), artifactFiles);
    } else {
      throw new FileNotFoundException("unable to collect artifact ");
    }
  }
}
