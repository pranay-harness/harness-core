package software.wings.graphql.schema.mutation.cloudProvider;

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
public class QLGcpCloudProviderInput {
  private RequestField<String> name;
  private RequestField<String> serviceAccountKeySecretId;
  private RequestField<String> delegateSelector;
  private RequestField<Boolean> skipValidation;
  private RequestField<Boolean> useDelegate;
}
