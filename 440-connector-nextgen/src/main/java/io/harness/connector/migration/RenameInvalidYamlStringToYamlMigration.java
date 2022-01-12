/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class RenameInvalidYamlStringToYamlMigration implements NGMigration {
  @Inject private MongoTemplate mongoTemplate;
  private static final int BATCH_SIZE = 100;

  private CloseableIterator<Connector> getIterator(Query query) {
    return mongoTemplate.stream(query, Connector.class);
  }

  @Override
  public void migrate() {
    Query query = new Query(new Criteria());
    query.cursorBatchSize(BATCH_SIZE);
    CloseableIterator<Connector> iterator = getIterator(query);

    log.info("Starting migration to rename invalid yaml string field to yaml for connectors");
    while (iterator.hasNext()) {
      try {
        Connector connector = iterator.next();
        if (connector.isEntityInvalid()) {
          connector.setYaml(connector.getInvalidYamlString());
          connector.setInvalidYamlString(null);
          mongoTemplate.save(connector);
        }
      } catch (Exception exception) {
        log.error("unable to run rename invalid yaml string to yaml for entity, ignoring it...", exception);
      }
    }
    log.info("Migration to rename invalid yaml string field to yaml for connectors completed successfully");
  }
}
