package software.wings.graphql.datafetcher.secretManager;

import software.wings.graphql.schema.mutation.secretManager.QLCreateSecretManagerInput;
import software.wings.graphql.schema.mutation.secretManager.QLUpdateSecretManagerInput;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager;

public interface SecretManagerMutationDataFetcher {
  QLSecretManager createSecretManager(QLCreateSecretManagerInput input, String accountId);

  QLSecretManager updateSecretManager(QLUpdateSecretManagerInput input, String accountId);

  void deleteSecretManager(String accountId, String secretManagerId);
}
