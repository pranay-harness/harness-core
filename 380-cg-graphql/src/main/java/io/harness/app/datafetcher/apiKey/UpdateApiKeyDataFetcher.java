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
