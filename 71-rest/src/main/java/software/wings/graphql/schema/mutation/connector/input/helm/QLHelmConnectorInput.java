package software.wings.graphql.schema.mutation.connector.input.helm;

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
public class QLHelmConnectorInput {
  private RequestField<String> name;
  private RequestField<QLAmazonS3PlatformInput> amazonS3PlatformDetails;
  private RequestField<QLGCSPlatformInput> gcsPlatformDetails;
  private RequestField<QLHttpServerPlatformInput> httpServerPlatformDetails;
}
