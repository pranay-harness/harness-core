/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.privileged.persistence.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDBO;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface PrivilegedRoleAssignmentCustomRepository {
  long insertAllIgnoringDuplicates(@NotNull List<PrivilegedRoleAssignmentDBO> assignments);
  List<PrivilegedRoleAssignmentDBO> get(@NotNull Criteria criteria);
  long remove(@NotNull Criteria criteria);
}
