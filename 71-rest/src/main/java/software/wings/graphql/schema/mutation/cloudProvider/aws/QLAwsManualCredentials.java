package software.wings.graphql.schema.mutation.cloudProvider.aws;

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
public class QLAwsManualCredentials {
  private RequestField<String> accessKey;
  private RequestField<String> accessKeySecretId;
  private RequestField<String> secretKeySecretId;
}
