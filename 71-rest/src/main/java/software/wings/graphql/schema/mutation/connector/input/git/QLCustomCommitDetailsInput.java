package software.wings.graphql.schema.mutation.connector.input.git;

import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLCustomCommitDetailsInput {
  private RequestField<String> authorName;
  private RequestField<String> authorEmailId;
  private RequestField<String> commitMessage;
}
