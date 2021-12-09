package io.harness.ng.scim;

import software.wings.scim.PatchRequest;
import software.wings.scim.ScimGroup;
import software.wings.scim.ScimGroupService;
import software.wings.scim.ScimListResponse;

import javax.ws.rs.core.Response;

public class NGScimGroupServiceImpl implements ScimGroupService {
  @Override
  public ScimListResponse<ScimGroup> searchGroup(String filter, String accountId, Integer count, Integer startIndex) {
    return null;
  }

  @Override
  public Response updateGroup(String groupId, String accountId, ScimGroup scimGroup) {
    return null;
  }

  @Override
  public void deleteGroup(String groupId, String accountId) {}

  @Override
  public Response updateGroup(String groupId, String accountId, PatchRequest patchRequest) {
    return null;
  }

  @Override
  public ScimGroup getGroup(String groupId, String accountId) {
    return null;
  }

  @Override
  public ScimGroup createGroup(ScimGroup groupQuery, String accountId) {
    return null;
  }
}
