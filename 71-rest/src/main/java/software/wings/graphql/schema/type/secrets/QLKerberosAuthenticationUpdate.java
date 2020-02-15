package software.wings.graphql.schema.type.secrets;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLKerberosAuthenticationUpdate {
  RequestField<String> principal;
  RequestField<String> realm;
  RequestField<Integer> port;
  RequestField<QLTGTGenerationMethod> tgtGenerationMethod;
}
