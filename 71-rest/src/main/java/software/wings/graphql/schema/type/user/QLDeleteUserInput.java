package software.wings.graphql.schema.type.user;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;

@Value
@Builder
public class QLDeleteUserInput implements QLMutationInput {
  private String clientMutationId;
  private String id;
}
