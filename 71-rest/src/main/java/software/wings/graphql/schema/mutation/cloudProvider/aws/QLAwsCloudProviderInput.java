package software.wings.graphql.schema.mutation.cloudProvider.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLAwsCloudProviderInput {
  private RequestField<String> name;
  private RequestField<QLUsageScope> usageScope;

  private RequestField<Boolean> useEc2IamCredentials;
  private RequestField<QLEc2IamCredentials> ec2IamCredentials;
  private RequestField<QLAwsManualCredentials> manualCredentials;

  private RequestField<Boolean> assumeCrossAccountRole;
  private RequestField<QLAwsCrossAccountAttributes> crossAccountAttributes;
}
