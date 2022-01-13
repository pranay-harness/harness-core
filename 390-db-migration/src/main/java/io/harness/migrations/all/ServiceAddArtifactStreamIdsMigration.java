/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceAddArtifactStreamIdsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    log.info("Add artifactStreamIds to Services");
    final DBCollection collection = wingsPersistence.getCollection(Service.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 0;
    int total = 0;
    log.info("Adding artifactServiceIds to Services");
    try (HIterator<Service> services = new HIterator<>(wingsPersistence.createQuery(Service.class).fetch())) {
      while (services.hasNext()) {
        Service service = services.next();
        if (i >= 50) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("Services: {} updated", total);
          i = 0;
        }

        List<String> artifactStreamIds = wingsPersistence.createQuery(ArtifactStream.class)
                                             .filter("appId", service.getAppId())
                                             .filter("serviceId", service.getUuid())
                                             .asList()
                                             .stream()
                                             .map(ArtifactStream::getUuid)
                                             .collect(Collectors.toList());
        if (!isEmpty(artifactStreamIds)) {
          bulkWriteOperation
              .find(wingsPersistence.createQuery(Service.class).filter(Service.ID, service.getUuid()).getQueryObject())
              .updateOne(
                  new BasicDBObject("$set", new BasicDBObject(ServiceKeys.artifactStreamIds, artifactStreamIds)));
          ++i;
          ++total;
        }
      }
    }
    if (i != 0) {
      bulkWriteOperation.execute();
      log.info("Services: {} updated", total);
    }
    log.info("Adding artifactServiceIds to Services completed");
  }
}
