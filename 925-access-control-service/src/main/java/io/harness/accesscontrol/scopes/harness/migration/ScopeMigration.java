/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.scopes.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Singleton
@OwnedBy(PL)
public class ScopeMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final ScopeService scopeService;

  @Inject
  public ScopeMigration(RoleAssignmentRepository roleAssignmentRepository, ScopeService scopeService) {
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.scopeService = scopeService;
  }

  @Override
  public void migrate() {
    int pageSize = 1000;
    int pageIndex = 0;
    long totalPages;
    Set<Scope> scopes = new HashSet<>();
    do {
      Pageable pageable = PageRequest.of(pageIndex, pageSize);
      Page<RoleAssignmentDBO> roleAssignmentDBOPage = roleAssignmentRepository.findAll(pageable);
      pageIndex++;
      totalPages = roleAssignmentDBOPage.getTotalPages();

      List<RoleAssignmentDBO> roleAssignmentList = roleAssignmentDBOPage.getContent();
      if (isEmpty(roleAssignmentList)) {
        return;
      }
      for (RoleAssignmentDBO roleAssignment : roleAssignmentList) {
        Scope scope = scopeService.buildScopeFromScopeIdentifier(roleAssignment.getScopeIdentifier());
        scopes.add(scope);
      }
      if (scopes.size() >= 100) {
        scopeService.saveAll(new ArrayList<>(scopes));
        scopes = new HashSet<>();
      }
    } while (pageIndex < totalPages);
    scopeService.saveAll(new ArrayList<>(scopes));
  }
}
