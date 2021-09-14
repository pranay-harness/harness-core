/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.privileged.persistence.repositories;

import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDBO;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public interface PrivilegedRoleAssignmentRepository
    extends PagingAndSortingRepository<PrivilegedRoleAssignmentDBO, String>, PrivilegedRoleAssignmentCustomRepository {}
