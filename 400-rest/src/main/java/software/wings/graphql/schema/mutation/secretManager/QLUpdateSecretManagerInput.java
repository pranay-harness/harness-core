package software.wings.graphql.schema.mutation.secretManager;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateSecretManagerInput implements QLMutationInput {
  String clientMutationId;
  String secretManagerId;
  QLSecretManagerType secretManagerType;
  QLUpdateHashicorpVaultInput hashicorpVaultConfigInput;
}
