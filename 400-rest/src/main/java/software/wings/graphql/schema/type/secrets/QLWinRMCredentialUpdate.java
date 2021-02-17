package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.beans.WinRmConnectionAttributes;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWinRMCredentialUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLWinRMCredentialUpdate {
  RequestField<String> name;
  RequestField<WinRmConnectionAttributes.AuthenticationScheme> authenticationScheme;
  RequestField<String> domain;
  RequestField<String> userName;
  RequestField<String> passwordSecretId;
  RequestField<Boolean> useSSL;
  RequestField<Boolean> skipCertCheck;
  RequestField<Integer> port;
  RequestField<QLUsageScope> usageScope;
}
