/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.scim.service;

import io.harness.scim.PatchRequest;
import io.harness.scim.ScimGroup;
import io.harness.scim.ScimListResponse;

import javax.ws.rs.core.Response;

public interface ScimGroupService {
  ScimListResponse<ScimGroup> searchGroup(String filter, String accountId, Integer count, Integer startIndex);

  Response updateGroup(String groupId, String accountId, ScimGroup scimGroup);

  void deleteGroup(String groupId, String accountId);

  Response updateGroup(String groupId, String accountId, PatchRequest patchRequest);

  ScimGroup getGroup(String groupId, String accountId);

  ScimGroup createGroup(ScimGroup groupQuery, String accountId);
}
