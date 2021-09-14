/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.ApiKeyEntry;
import software.wings.beans.security.UserGroup;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

public interface ApiKeyService extends OwnedByAccount {
  ApiKeyEntry update(String uuid, String accountId, ApiKeyEntry apiKeyEntry);
  PageResponse<ApiKeyEntry> list(
      PageRequest<ApiKeyEntry> request, String accountId, boolean loadUserGroups, boolean decrypt);
  ApiKeyEntry generate(@NotEmpty String accountId, ApiKeyEntry apiKeyEntry);
  void delete(String accountId, @NotEmpty String uuid);

  ApiKeyEntry get(@NotEmpty String uuid, @NotEmpty String accountId);
  void validate(@NotEmpty String key, @NotEmpty String accountId);
  String getAccountIdFromApiKey(String apiKey);
  ApiKeyEntry getByKey(String key, String accountId, boolean details);
  UserPermissionInfo getApiKeyPermissions(ApiKeyEntry apiKeyEntry, String accountId);
  UserRestrictionInfo getApiKeyRestrictions(
      ApiKeyEntry apiKeyEntry, UserPermissionInfo userPermissionInfo, String accountId);
  void evictAndRebuildPermissions(String accountId, boolean rebuild);
  void evictAndRebuildPermissionsAndRestrictions(String accountId, boolean rebuild);
  void evictPermissionsAndRestrictionsForUserGroup(UserGroup userGroup);
  void loadUserGroupsForApiKeys(List<ApiKeyEntry> apiKeyEntries, String accountId);
  Boolean isApiKeyValid(@NotEmpty String key, @NotEmpty String accountId);
}
