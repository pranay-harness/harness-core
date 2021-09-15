/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.scim;

import software.wings.beans.scim.ScimUser;

import javax.ws.rs.core.Response;

public interface ScimUserService {
  Response createUser(ScimUser userQuery, String accountId);

  ScimUser getUser(String userId, String accountId);

  ScimListResponse<ScimUser> searchUser(String accountId, String filter, Integer count, Integer startIndex);

  void deleteUser(String userId, String accountId);

  ScimUser updateUser(String accountId, String userId, PatchRequest patchRequest);

  Response updateUser(String userId, String accountId, ScimUser scimUser);
}
