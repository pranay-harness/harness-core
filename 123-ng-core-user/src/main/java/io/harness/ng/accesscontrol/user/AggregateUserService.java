/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.accesscontrol.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;

@OwnedBy(PL)
public interface AggregateUserService {
  PageResponse<UserAggregateDTO> getAggregatedUsers(Scope scope, String searchTerm, PageRequest pageRequest);

  PageResponse<UserAggregateDTO> getAggregatedUsers(
      Scope scope, ACLAggregateFilter aclAggregateFilter, PageRequest pageRequest);

  UserAggregateDTO getAggregatedUser(Scope scope, String userId);
}
