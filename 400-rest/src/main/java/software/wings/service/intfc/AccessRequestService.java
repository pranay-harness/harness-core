package software.wings.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.security.AccessRequest;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface AccessRequestService {
  AccessRequest create(
      String accountId, String harnessUserGroupId, long accessStartAt, long accessEndAt, boolean accessActive);

  AccessRequest get(String accessRequestId);

  List<AccessRequest> getActiveAccessRequest(String harnessUserGroupId);

  List<AccessRequest> getActiveAccessRequestForAccount(String accountId);

  boolean update(String accessRequestId, long accessStartAt, long accessEndAt);

  boolean updateStatus(String accessRequestId, boolean accessStatus);

  boolean delete(String accessRequestId);
}
