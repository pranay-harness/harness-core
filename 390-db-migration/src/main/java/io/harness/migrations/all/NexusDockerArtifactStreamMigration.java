/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.artifact.ArtifactUtilities;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.NexusConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NexusDockerArtifactStreamMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(ArtifactStream.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    log.info("Migrating Nexus Docker Artifact Streams");
    try (HIterator<ArtifactStream> nexusArtifactStreams =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                 .filter(ArtifactStreamKeys.artifactStreamType, ArtifactStreamType.NEXUS.name())
                                 .fetch())) {
      while (nexusArtifactStreams.hasNext()) {
        NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) nexusArtifactStreams.next();
        if (i % 50 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("Artifact Streams: {} updated", i);
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
                          .filter(ArtifactStream.ID_KEY2, nexusArtifactStream.getUuid())
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
      log.info("Artifact Streams: {} updated", i);
    }
    log.info("Migrating Nexus Docker Artifact Streams completed");
  }
}
