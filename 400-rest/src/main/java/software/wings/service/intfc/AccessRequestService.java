package software.wings.service.intfc;

import software.wings.beans.security.AccessRequest;

import java.util.List;

public interface AccessRequestService {
  AccessRequest create(
      String accountId, String harnessUserGroupId, long accessStartAt, long accessEndAt, boolean accessActive);

  AccessRequest get(String accessRequestId);

  List<AccessRequest> getActiveAccessRequest(String harnessUserGroupId);

  List<AccessRequest> getActiveAccessRequestForAccount(String accountId);

  AccessRequest update(String accessRequestId, long startTime, long endTime);

  AccessRequest updateStatus(String accessRequestId, boolean accessStatus);

  boolean delete(String accessRequestId);
}
