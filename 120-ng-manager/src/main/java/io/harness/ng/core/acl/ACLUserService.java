package io.harness.ng.core.acl;

import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

public interface ACLUserService {
  PageResponse<ACLUserAggregateDTO> getActiveUsers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest);

  PageResponse<ACLUserAggregateDTO> getPendingUsers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId);
}
