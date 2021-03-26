package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.UserGroupAggregateDTO;

@OwnedBy(PL)
public interface AggregateUserGroupService {
  PageResponse<UserGroupAggregateDTO> listAggregateUserGroups(PageRequest pageRequest, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, AggregateACLRequest aggregateACLRequest);
}
