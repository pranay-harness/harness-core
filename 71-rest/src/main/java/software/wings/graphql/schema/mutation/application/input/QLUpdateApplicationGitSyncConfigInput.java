package software.wings.graphql.schema.mutation.application.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationGitSyncConfigInputKeys")
public class QLUpdateApplicationGitSyncConfigInput implements QLMutationInput {
  private String clientMutationId;
  private String applicationId;
  private String gitConnectorId;
  private String branch;
  private Boolean syncEnabled;
}
