package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.UserGroupAggregateDTO;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface AggregateUserGroupService {
  PageResponse<UserGroupAggregateDTO> listAggregateUserGroups(@NotNull PageRequest pageRequest,
      @NotEmpty String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull AggregateACLRequest aggregateACLRequest);

  UserGroupAggregateDTO getAggregatedUserGroup(@NotEmpty String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotEmpty String userGroupIdentifier);
}
