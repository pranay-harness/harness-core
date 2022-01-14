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
