/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.user.service.impl;

import io.harness.beans.Scope;
import io.harness.migration.NGMigration;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.entities.UserMembership;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserMembershipStaleScopeMigrationService implements NGMigration {
  private static final int BATCH_SIZE = 20;
  MongoTemplate mongoTemplate;
  OrganizationService organizationService;
  ProjectService projectService;
  Set<Scope> deletedScopes;
  Set<Scope> activeScopes;

  @Inject
  public UserMembershipStaleScopeMigrationService(
      MongoTemplate mongoTemplate, OrganizationService organizationService, ProjectService projectService) {
    this.mongoTemplate = mongoTemplate;
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.deletedScopes = new HashSet<>();
    this.activeScopes = new HashSet<>();
  }

  private CloseableIterator<UserMembership> runQueryWithBatch(Criteria criteria, int batchSize) {
    Query query = new Query(criteria);
    query.cursorBatchSize(batchSize);
    return mongoTemplate.stream(query, UserMembership.class);
  }

  @Override
  public void migrate() {
    log.info("Deleting UserMemberships with stale scopes");
    CloseableIterator<UserMembership> iterator = runQueryWithBatch(new Criteria(), BATCH_SIZE);
    while (iterator.hasNext()) {
      UserMembership userMembership = iterator.next();
      if (!isScopeActive(userMembership.getScope())) {
        log.info("removing usermembership {}", userMembership);
        mongoTemplate.remove(userMembership);
      }
    }
    log.info("UserMemberships with stale scopes deleted");
  }

  private boolean isScopeActive(Scope scope) {
    if (deletedScopes.contains(scope)) {
      return false;
    }
    if (activeScopes.contains(scope)) {
      return true;
    }

    boolean isScopeActive = true;
    if (StringUtils.isNotBlank(scope.getProjectIdentifier())) {
      isScopeActive =
          projectService.get(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier())
              .isPresent();
    } else if (StringUtils.isNotBlank(scope.getOrgIdentifier())) {
      isScopeActive = organizationService.get(scope.getAccountIdentifier(), scope.getOrgIdentifier()).isPresent();
    }

    if (isScopeActive) {
      activeScopes.add(scope);
    } else {
      deletedScopes.add(scope);
    }
    return isScopeActive;
  }
}
