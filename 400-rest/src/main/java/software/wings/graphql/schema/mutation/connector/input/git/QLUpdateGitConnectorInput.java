package software.wings.graphql.schema.mutation.connector.input.git;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateGitConnectorInput {
  private RequestField<String> name;
  private RequestField<String> userName;
  private RequestField<String> URL;
  private RequestField<String> branch;
  private RequestField<String> passwordSecretId;
  private RequestField<String> sshSettingId;
  private RequestField<Boolean> generateWebhookUrl;
  private RequestField<QLCustomCommitDetailsInput> customCommitDetails;
}
