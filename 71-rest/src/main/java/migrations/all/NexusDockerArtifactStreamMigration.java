package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.artifact.ArtifactUtilities;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.NexusConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class NexusDockerArtifactStreamMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(ArtifactStream.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    logger.info("Migrating Nexus Docker Artifact Streams");
    try (HIterator<ArtifactStream> nexusArtifactStreams =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                 .filter(ArtifactStream.ARTIFACT_STREAM_TYPE_KEY, ArtifactStreamType.NEXUS.name())
                                 .fetch())) {
      while (nexusArtifactStreams.hasNext()) {
        NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) nexusArtifactStreams.next();
        if (i % 50 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Artifact Streams: {} updated", i);
        }
        if (isEmpty(nexusArtifactStream.getArtifactPaths())) {
          SettingAttribute settingAttribute = settingsService.get(nexusArtifactStream.getSettingId());
          if (settingAttribute != null && settingAttribute.getValue() != null
              && settingAttribute.getValue() instanceof NexusConfig) {
            NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
            String dockerRegistryUrl = ArtifactUtilities.getNexusRegistryUrl(nexusConfig.getNexusUrl(),
                nexusArtifactStream.getDockerPort(), nexusArtifactStream.getDockerRegistryUrl());
            bulkWriteOperation
                .find(wingsPersistence.createQuery(ArtifactStream.class)
                          .filter(ArtifactStream.ID_KEY, nexusArtifactStream.getUuid())
                          .getQueryObject())
                .updateOne(new BasicDBObject(
                    "$set", new BasicDBObject(NexusArtifactStream.DOCKER_REGISTRY_URL_KEY, dockerRegistryUrl)));
            ++i;
          }
        }
      }
    }
    if (i % 50 != 1) {
      bulkWriteOperation.execute();
      logger.info("Artifact Streams: {} updated", i);
    }
    logger.info("Migrating Nexus Docker Artifact Streams completed");
  }
}
