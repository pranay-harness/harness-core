package software.wings.graphql.schema.mutation.secretManager;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLDeleteSecretManagerInput implements QLMutationInput {
  String clientMutationId;
  String secretManagerId;
}
