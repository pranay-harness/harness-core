package io.harness.ng.accesscontrol.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

@OwnedBy(PL)
public interface UserService {
  PageResponse<UserAggregateDTO> getUsers(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter);

  boolean deleteUser(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId);
}
