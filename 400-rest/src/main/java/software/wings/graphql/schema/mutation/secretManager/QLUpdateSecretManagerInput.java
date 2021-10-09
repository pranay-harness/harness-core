package software.wings.graphql.schema.mutation.secretManager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateSecretManagerInput implements QLMutationInput {
  String clientMutationId;
  String secretManagerId;
  QLSecretManagerType secretManagerType;
  QLUpdateHashicorpVaultInput hashicorpVaultConfigInput;
  QLUpdateCustomSecretManagerInput customSecretManagerInput;
}
