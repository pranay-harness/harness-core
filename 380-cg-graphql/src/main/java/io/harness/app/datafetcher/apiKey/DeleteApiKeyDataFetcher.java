/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app.datafetcher.apiKey;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.apiKey.input.QLDeleteApiKeyInput;
import io.harness.app.schema.mutation.apiKey.payload.QLDeleteApiKeyPayload;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ApiKeyService;

import com.google.inject.Inject;

@OwnedBy(PL)
public class DeleteApiKeyDataFetcher extends BaseMutatorDataFetcher<QLDeleteApiKeyInput, QLDeleteApiKeyPayload> {
  @Inject public ApiKeyService apiKeyService;

  @Inject
  DeleteApiKeyDataFetcher(ApiKeyService apiKeyService) {
    super(QLDeleteApiKeyInput.class, QLDeleteApiKeyPayload.class);
    this.apiKeyService = apiKeyService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_API_KEYS)
  public QLDeleteApiKeyPayload mutateAndFetch(QLDeleteApiKeyInput parameter, MutationContext mutationContext) {
    String accountId = parameter.getAccountId();
    String uuid = parameter.getUuid();
    apiKeyService.delete(accountId, uuid);
    return QLDeleteApiKeyPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
