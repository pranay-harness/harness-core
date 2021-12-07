package io.harness.ng.scim;

import software.wings.beans.scim.ScimUser;
import software.wings.scim.PatchRequest;
import software.wings.scim.ScimListResponse;

import javax.ws.rs.core.Response;

public interface NGScimUserService {
  Response createUser(ScimUser userQuery, String accountId);

  ScimUser getUser(String userId, String accountId);

  ScimListResponse<ScimUser> searchUser(String accountId, String filter, Integer count, Integer startIndex);

  void deleteUser(String userId, String accountId);

  ScimUser updateUser(String accountId, String userId, PatchRequest patchRequest);

  Response updateUser(String userId, String accountId, ScimUser scimUser);
}
