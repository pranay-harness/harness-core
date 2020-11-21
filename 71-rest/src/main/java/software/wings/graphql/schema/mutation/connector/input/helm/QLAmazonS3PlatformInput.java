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
public class QLAmazonS3PlatformInput {
  private RequestField<String> awsCloudProvider;
  private RequestField<String> bucketName;
  private RequestField<String> region;
}
