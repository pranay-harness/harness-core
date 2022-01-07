/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import static io.harness.ng.core.entities.Organization.OrganizationKeys;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.repositories.core.spring.OrganizationRepository;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NGDefaultOrgNameMigration implements NGMigration {
  private OrganizationRepository organizationRepository;

  private final String HARNESS_DEFAULT_ORG_NEW_NAME = "default";
  private final String HARNESS_DEFAULT_ORG_OLD_NAME = "Default";

  @Inject
  public NGDefaultOrgNameMigration(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  @Override
  public void migrate() {
    try {
      log.info("[NGDefaultOrgNameMigration] Updating Default Org Name from:{} to:{}", HARNESS_DEFAULT_ORG_OLD_NAME,
          HARNESS_DEFAULT_ORG_NEW_NAME);
      Criteria criteria = new Criteria();
      criteria.and(OrganizationKeys.name).is(HARNESS_DEFAULT_ORG_OLD_NAME);
      criteria.and(OrganizationKeys.harnessManaged).is(true);
      Update update = new Update();
      update.set(OrganizationKeys.name, HARNESS_DEFAULT_ORG_NEW_NAME);
      Query query = new Query(criteria);
      log.info("[NGDefaultOrgNameMigration] Query for updating Harness Default Org Name from:{} to:{} is: {}",
          HARNESS_DEFAULT_ORG_OLD_NAME, HARNESS_DEFAULT_ORG_NEW_NAME, query.getQueryObject().toJson());
      UpdateResult result = organizationRepository.updateMultiple(query, update);
      log.info("[NGDefaultSMNameMigration] Successfully updated {} Harness Default SM Name from:{} to:{}",
          result.getModifiedCount(), HARNESS_DEFAULT_ORG_OLD_NAME, HARNESS_DEFAULT_ORG_NEW_NAME);
    } catch (Exception e) {
      log.error(
          "[NGDefaultSMNameMigration] Migration for changing name of Harness Default Org failed with error ,Ignoring the error",
          e);
    }
  }
}
