/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
