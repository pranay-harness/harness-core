package software.wings.graphql.schema.mutation.cloudProvider.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.cloudProvider.aws.QLAwsCredentialsType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateAwsCloudProviderInput {
  private RequestField<String> name;

  private RequestField<QLAwsCredentialsType> credentialsType;
  private RequestField<QLUpdateEc2IamCredentials> ec2IamCredentials;
  private RequestField<QLUpdateAwsManualCredentials> manualCredentials;

  private RequestField<QLUpdateAwsCrossAccountAttributes> crossAccountAttributes;
}
