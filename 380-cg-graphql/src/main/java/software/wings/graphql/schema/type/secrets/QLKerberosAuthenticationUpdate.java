package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
@OwnedBy(HarnessTeam.PL)
public class QLKerberosAuthenticationUpdate {
  RequestField<String> principal;
  RequestField<String> realm;
  RequestField<Integer> port;
  RequestField<QLTGTGenerationMethod> tgtGenerationMethod;
}
