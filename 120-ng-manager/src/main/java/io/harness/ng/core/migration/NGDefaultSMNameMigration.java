/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.connector.entities.Connector.ConnectorKeys;
import static io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector.GcpKmsConnectorKeys;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.repositories.ConnectorRepository;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@OwnedBy(PL)
public class NGDefaultSMNameMigration implements NGMigration {
  private ConnectorRepository connectorRepository;
  private final String HARNESS_DEFAULT_SM_NEW_NAME = "Harness Built-in Secret Manager";
  private final String HARNESS_DEFAULT_SM_OLD_NAME = "Harness Secrets Manager Google KMS";

  @Inject
  public NGDefaultSMNameMigration(ConnectorRepository connectorRepository) {
    this.connectorRepository = connectorRepository;
  }

  @Override
  public void migrate() {
    try {
      log.info("[NGDefaultSMNameMigration] Updating Harness Default SM Name from:{} to:{}", HARNESS_DEFAULT_SM_OLD_NAME,
          HARNESS_DEFAULT_SM_NEW_NAME);
      Criteria criteria = new Criteria();
      criteria.and(ConnectorKeys.name).is(HARNESS_DEFAULT_SM_OLD_NAME);
      criteria.and(GcpKmsConnectorKeys.harnessManaged).is(true);
      Update update = new Update();
      update.set(ConnectorKeys.name, HARNESS_DEFAULT_SM_NEW_NAME);
      Query query = new Query(criteria);
      log.info("[NGDefaultSMNameMigration] Query for updating Harness Default SM Name from:{} to:{} is: {}",
          HARNESS_DEFAULT_SM_OLD_NAME, HARNESS_DEFAULT_SM_NEW_NAME, query.getQueryObject().toJson());
      UpdateResult result = connectorRepository.updateMultiple(query, update);
      log.info("[NGDefaultSMNameMigration] Successfully updated {} Harness Default SM Name from:{} to:{}",
          result.getModifiedCount(), HARNESS_DEFAULT_SM_OLD_NAME, HARNESS_DEFAULT_SM_NEW_NAME);
    } catch (Exception e) {
      log.error(
          "[NGDefaultSMNameMigration] Migration for changing name of Harness Default SM failed with error ,Ignoring the error",
          e);
    }
  }
}
