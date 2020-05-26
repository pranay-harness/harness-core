package software.wings.graphql.schema.mutation.cloudProvider.aws;

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
public class QLAwsCrossAccountAttributes {
  private RequestField<String> crossAccountRoleArn;
  private RequestField<String> externalId;
}
