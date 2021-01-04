package software.wings.graphql.schema.mutation.application.input;

import software.wings.graphql.schema.mutation.QLMutationInput;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationGitSyncConfigInputKeys")
public class QLUpdateApplicationGitSyncConfigInput implements QLMutationInput {
  private String clientMutationId;
  private String applicationId;
  private String gitConnectorId;
  private String branch;
  private String repositoryName;
  private Boolean syncEnabled;
}
