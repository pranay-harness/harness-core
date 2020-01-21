package software.wings.graphql.schema.type.user;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;

@Value
@Builder
public class QLUpdateUserInput implements QLMutationInput {
  private String id;
  private String requestId;
  private RequestField<String> name;
}
