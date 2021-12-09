package io.harness.ng.scim;

import software.wings.beans.scim.ScimUser;
import software.wings.scim.PatchRequest;
import software.wings.scim.ScimListResponse;
import software.wings.scim.ScimUserService;

import javax.ws.rs.core.Response;

public class NGScimUserServiceImpl implements ScimUserService {
  @Override
  public Response createUser(ScimUser userQuery, String accountId) {
    return null;
  }

  @Override
  public ScimUser getUser(String userId, String accountId) {
    return null;
  }

  @Override
  public ScimListResponse<ScimUser> searchUser(String accountId, String filter, Integer count, Integer startIndex) {
    return null;
  }

  @Override
  public void deleteUser(String userId, String accountId) {}

  @Override
  public ScimUser updateUser(String accountId, String userId, PatchRequest patchRequest) {
    return null;
  }

  @Override
  public Response updateUser(String userId, String accountId, ScimUser scimUser) {
    return null;
  }
}
