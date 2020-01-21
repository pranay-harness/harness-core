package software.wings.graphql.schema.mutation.application.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationGitConfigInputKeys")
public class QLUpdateApplicationGitConfigInput implements QLMutationInput {
  private String requestId;
  private String applicationId;
  private String gitConnectorId;
  private String branch;
  private Boolean syncEnabled;
}
