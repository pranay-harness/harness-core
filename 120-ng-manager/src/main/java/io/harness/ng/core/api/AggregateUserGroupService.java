/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.UserGroupAggregateDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface AggregateUserGroupService {
  PageResponse<UserGroupAggregateDTO> listAggregateUserGroups(@NotNull PageRequest pageRequest,
      @NotEmpty String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm,
      int userSize);

  List<UserGroupAggregateDTO> listAggregateUserGroups(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull AggregateACLRequest aggregateACLRequest);

  UserGroupAggregateDTO getAggregatedUserGroup(@NotEmpty String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotEmpty String userGroupIdentifier);
}
