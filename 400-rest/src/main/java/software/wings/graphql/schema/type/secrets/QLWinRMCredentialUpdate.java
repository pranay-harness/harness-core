package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDP)
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
