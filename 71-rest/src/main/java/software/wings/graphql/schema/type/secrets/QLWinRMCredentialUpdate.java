package software.wings.graphql.schema.type.secrets;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWinRMCredentialUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLWinRMCredentialUpdate {
  RequestField<String> name;
  RequestField<WinRmConnectionAttributes.AuthenticationScheme> authenticationScheme;
  RequestField<String> domain;
  RequestField<String> userName;
  RequestField<String> password;
  RequestField<Boolean> useSSL;
  RequestField<Boolean> skipCertCheck;
  RequestField<Integer> port;
}
