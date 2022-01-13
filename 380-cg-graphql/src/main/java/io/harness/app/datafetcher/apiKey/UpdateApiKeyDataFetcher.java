/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app.datafetcher.apiKey;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.apiKey.input.QLUpdateApiKeyInput;
import io.harness.app.schema.mutation.apiKey.payload.QLUpdateApiKeyPayload;

import software.wings.beans.ApiKeyEntry;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ApiKeyService;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(PL)
public class UpdateApiKeyDataFetcher extends BaseMutatorDataFetcher<QLUpdateApiKeyInput, QLUpdateApiKeyPayload> {
  @Inject public ApiKeyService apiKeyService;

  @Inject
  UpdateApiKeyDataFetcher(ApiKeyService apiKeyService) {
    super(QLUpdateApiKeyInput.class, QLUpdateApiKeyPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_API_KEYS)
  public QLUpdateApiKeyPayload mutateAndFetch(QLUpdateApiKeyInput parameter, MutationContext mutationContext) {
    String accountId = parameter.getAccountId();
    String name = parameter.getName();
    String uuid = parameter.getUuid();
    List<String> userGroupIds = parameter.getUserGroupIds();
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().accountId(accountId).name(name).userGroupIds(userGroupIds).build();
    ApiKeyEntry result = apiKeyService.update(uuid, accountId, apiKeyEntry);
    return QLUpdateApiKeyPayload.builder()
        .clientMutationId(parameter.getClientMutationId())
        .uuid(result.getUuid())
        .name(result.getName())
        .build();
  }
}
