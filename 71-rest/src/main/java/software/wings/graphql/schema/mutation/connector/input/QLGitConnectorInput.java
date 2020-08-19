package software.wings.graphql.schema.mutation.connector.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.GitConfig.UrlType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLGitConnectorInput {
  private RequestField<String> name;

  private RequestField<String> userName;
  private RequestField<String> URL;
  private RequestField<UrlType> urlType;
  private RequestField<String> branch;
  private RequestField<String> passwordSecretId;
  private RequestField<String> sshSettingId;
  private RequestField<Boolean> generateWebhookUrl;
  private RequestField<QLCustomCommitDetailsInput> customCommitDetails;
}
