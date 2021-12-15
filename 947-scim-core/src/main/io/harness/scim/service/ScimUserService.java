package io.harness.scim.service;

import io.harness.scim.PatchRequest;
import io.harness.scim.ScimListResponse;
import io.harness.scim.ScimUser;

import javax.ws.rs.core.Response;

public interface ScimUserService {
  Response createUser(ScimUser userQuery, String accountId);

  ScimUser getUser(String userId, String accountId);

  ScimListResponse<ScimUser> searchUser(String accountId, String filter, Integer count, Integer startIndex);

  void deleteUser(String userId, String accountId);

  ScimUser updateUser(String accountId, String userId, PatchRequest patchRequest);

  Response updateUser(String userId, String accountId, ScimUser scimUser);

  boolean changeScimUserDisabled(String accountId, String userId, boolean disabled);
}
