package software.wings.graphql.schema.mutation.cloudProvider.aws;

import io.harness.utils.RequestField;

import software.wings.graphql.schema.type.cloudProvider.aws.QLAwsCredentialsType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLAwsCloudProviderInput {
  private RequestField<String> name;

  private RequestField<QLAwsCredentialsType> credentialsType;
  private RequestField<QLEc2IamCredentials> ec2IamCredentials;
  private RequestField<QLAwsManualCredentials> manualCredentials;

  private RequestField<QLAwsCrossAccountAttributes> crossAccountAttributes;
  private RequestField<String> defaultRegion;
}
