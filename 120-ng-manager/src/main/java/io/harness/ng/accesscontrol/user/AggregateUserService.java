package io.harness.ng.accesscontrol.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;

@OwnedBy(PL)
public interface AggregateUserService {
  PageResponse<UserAggregateDTO> getAggregatedUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter);

  UserAggregateDTO getAggregatedUser(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
