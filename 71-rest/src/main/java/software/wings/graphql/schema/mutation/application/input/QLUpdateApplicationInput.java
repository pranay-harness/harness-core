package software.wings.graphql.schema.mutation.application.input;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationInputKeys")
public class QLUpdateApplicationInput implements QLMutationInput {
  private String clientMutationId;
  private String applicationId;
  private RequestField<String> name;
  private RequestField<String> description;
}
