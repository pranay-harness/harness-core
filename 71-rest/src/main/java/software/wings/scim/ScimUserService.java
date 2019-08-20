package software.wings.scim;

import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.types.UserResource;

import javax.ws.rs.core.Response;

public interface ScimUserService {
  Response createUser(UserResource userQuery, String accountId);

  UserResource getUser(String userId, String accountId);

  ListResponse<UserResource> searchUser(String accountId, String filter, Integer count, Integer startIndex);

  void deleteUser(String userId, String accountId);

  UserResource updateUser(String accountId, String userId, PatchRequest patchRequest);

  Response updateUser(String userId, String accountId, UserResource userResource);
}
