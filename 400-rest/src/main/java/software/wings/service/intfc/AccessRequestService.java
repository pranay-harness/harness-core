package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.security.AccessRequest;
import software.wings.beans.security.AccessRequestDTO;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
@TargetModule(_970_RBAC_CORE)
public interface AccessRequestService {
  AccessRequest createAccessRequest(AccessRequestDTO accessRequestDTO);

  AccessRequest get(String accessRequestId);

  List<AccessRequest> getActiveAccessRequest(String harnessUserGroupId);

  List<AccessRequest> getActiveAccessRequestForAccount(String accountId);

  List<AccessRequest> getActiveAccessRequestForAccountAndUser(String accountId, String userId);

  boolean delete(String accessRequestId);

  void checkAndUpdateAccessRequests(AccessRequest accessRequest);
}
