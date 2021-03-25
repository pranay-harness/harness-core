package io.harness.ng.core.acl.user;

import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.acl.user.remote.ACLAggregateFilter;

public interface ACLUserService {
  PageResponse<ACLUserAggregateDTO> getActiveUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter);

  PageResponse<ACLUserAggregateDTO> getPendingUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId);
}
