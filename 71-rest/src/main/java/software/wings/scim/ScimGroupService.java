package software.wings.scim;

import com.unboundid.scim2.common.messages.ListResponse;

import javax.ws.rs.core.Response;

public interface ScimGroupService {
  ListResponse<GroupResource> searchGroup(String filter, String accountId, Integer count, Integer startIndex);

  Response updateGroup(String groupId, String accountId, GroupResource groupResource);

  void deleteGroup(String groupId, String accountId);

  Response updateGroup(String groupId, String accountId, PatchRequest patchRequest);

  GroupResource getGroup(String groupId, String accountId);

  GroupResource createGroup(GroupResource groupQuery, String accountId);
}
