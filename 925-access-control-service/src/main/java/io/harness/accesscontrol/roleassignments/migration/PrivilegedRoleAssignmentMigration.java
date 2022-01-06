/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.roleassignments.privileged.AdminPrivilegedRoleAssignmentMapper;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedRoleAssignmentHandler;
import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDBO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(PL)
public class PrivilegedRoleAssignmentMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final PrivilegedRoleAssignmentHandler privilegedRoleAssignmentHandler;
  private final MongoTemplate mongoTemplate;

  @Inject
  public PrivilegedRoleAssignmentMigration(RoleAssignmentRepository roleAssignmentRepository,
      PrivilegedRoleAssignmentHandler privilegedRoleAssignmentHandler,
      @Named("mongoTemplate") MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.privilegedRoleAssignmentHandler = privilegedRoleAssignmentHandler;
  }

  @Override
  public void migrate() {
    try {
      mongoTemplate.getCollection(PrivilegedRoleAssignmentDBO.COLLECTION_NAME).dropIndex("uniqueIndex");
    } catch (MongoException e) {
      if (e.getCode() == 27) {
        log.info("uniqueIndex is already deleted");
      } else {
        throw new UnexpectedException("Migration failed. Could not delete uniqueIndex");
      }
    }
    int pageSize = 1000;
    int pageIndex = 0;
    Pageable pageable = PageRequest.of(pageIndex, pageSize);
    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.roleIdentifier)
                            .in(AdminPrivilegedRoleAssignmentMapper.roleToPrivilegedRole.keySet())
                            .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                            .in(AdminPrivilegedRoleAssignmentMapper.ALL_RESOURCES_RESOURCE_GROUP);
    long totalPages = roleAssignmentRepository.findAll(criteria, pageable).getTotalPages();
    do {
      pageable = PageRequest.of(pageIndex, pageSize);
      List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentRepository.findAll(criteria, pageable).getContent();
      if (isEmpty(roleAssignmentList)) {
        return;
      }
      for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
        privilegedRoleAssignmentHandler.handleRoleAssignmentCreate(roleAssignment);
      }
      pageIndex++;
    } while (pageIndex < totalPages);
  }
}
