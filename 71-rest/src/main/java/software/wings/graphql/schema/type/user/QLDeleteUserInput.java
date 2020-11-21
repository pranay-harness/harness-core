package software.wings.graphql.schema.type.user;

import software.wings.graphql.schema.mutation.QLMutationInput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeleteUserInput implements QLMutationInput {
  private String clientMutationId;
  private String id;
}
