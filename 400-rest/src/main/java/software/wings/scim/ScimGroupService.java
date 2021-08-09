package software.wings.scim;

import javax.ws.rs.core.Response;

public interface ScimGroupService {
  ScimListResponse<ScimGroup> searchGroup(String filter, String accountId, Integer count, Integer startIndex);

  Response updateGroup(String groupId, String accountId, ScimGroup scimGroup);

  void deleteGroup(String groupId, String accountId);

  Response updateGroup(String groupId, String accountId, PatchRequest patchRequest);

  ScimGroup getGroup(String groupId, String accountId);

  ScimGroup createGroup(ScimGroup groupQuery, String accountId);
}
