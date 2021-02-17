package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedTextUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLEncryptedTextUpdate {
  RequestField<String> name;
  RequestField<String> value;
  RequestField<String> secretReference;
  RequestField<Boolean> scopedToAccount;
  RequestField<QLUsageScope> usageScope;
  RequestField<Boolean> inheritScopesFromSM;
}
