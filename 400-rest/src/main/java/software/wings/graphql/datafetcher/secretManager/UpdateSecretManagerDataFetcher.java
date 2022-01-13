/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secretManager;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secretManager.QLUpdateSecretManagerInput;
import software.wings.graphql.schema.mutation.secretManager.QLUpsertSecretManagerPayload;
import software.wings.graphql.schema.mutation.secretManager.QLUpsertSecretManagerPayload.QLUpsertSecretManagerPayloadBuilder;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UpdateSecretManagerDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateSecretManagerInput, QLUpsertSecretManagerPayload> {
  @Inject
  public UpdateSecretManagerDataFetcher() {
    super(QLUpdateSecretManagerInput.class, QLUpsertSecretManagerPayload.class);
  }

  @Inject SecretManagerDataFetcherRegistry secretManagerDataFetcherRegistry;

  @Override
  @AuthRule(permissionType = MANAGE_SECRET_MANAGERS)
  protected QLUpsertSecretManagerPayload mutateAndFetch(
      QLUpdateSecretManagerInput input, MutationContext mutationContext) {
    QLUpsertSecretManagerPayloadBuilder payloadBuilder =
        QLUpsertSecretManagerPayload.builder().clientMutationId(input.getClientMutationId());

    SecretManagerMutationDataFetcher dataFetcher =
        secretManagerDataFetcherRegistry.getDataFetcher(input.getSecretManagerType().getEncryptionType());

    QLSecretManager secretManager = dataFetcher.updateSecretManager(input, mutationContext.getAccountId());

    payloadBuilder.secretManager(secretManager);

    return payloadBuilder.build();
  }
}
