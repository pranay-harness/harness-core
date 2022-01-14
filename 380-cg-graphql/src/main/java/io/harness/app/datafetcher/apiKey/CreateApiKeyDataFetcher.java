package io.harness.app.datafetcher.apiKey;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.apiKey.input.QLCreateApiKeyInput;
import io.harness.app.schema.mutation.apiKey.payload.QLCreateApiKeyPayload;

import software.wings.beans.ApiKeyEntry;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ApiKeyService;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(PL)
public class CreateApiKeyDataFetcher extends BaseMutatorDataFetcher<QLCreateApiKeyInput, QLCreateApiKeyPayload> {
  @Inject public ApiKeyService apiKeyService;

  @Inject
  CreateApiKeyDataFetcher(ApiKeyService apiKeyService) {
    super(QLCreateApiKeyInput.class, QLCreateApiKeyPayload.class);
    this.apiKeyService = apiKeyService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_API_KEYS)
  public QLCreateApiKeyPayload mutateAndFetch(QLCreateApiKeyInput parameter, MutationContext mutationContext) {
    String accountId = parameter.getAccountId();
    String name = parameter.getName();
    List<String> userGroupIds = parameter.getUserGroupIds();
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().accountId(accountId).name(name).userGroupIds(userGroupIds).build();
    ApiKeyEntry result = apiKeyService.generate(accountId, apiKeyEntry);
    return QLCreateApiKeyPayload.builder()
        .clientMutationId(parameter.getClientMutationId())
        .uuid(result.getUuid())
        .name(result.getName())
        .build();
  }
}