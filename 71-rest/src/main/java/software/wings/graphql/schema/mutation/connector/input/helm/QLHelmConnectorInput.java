package software.wings.graphql.schema.mutation.connector.input.helm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

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
