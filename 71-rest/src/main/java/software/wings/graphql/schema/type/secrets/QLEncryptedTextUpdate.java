package software.wings.graphql.schema.type.secrets;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedTextUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLEncryptedTextUpdate {
  RequestField<String> name;
  RequestField<String> value;
  RequestField<String> secretReference;
  RequestField<QLUsageScope> usageScope;
}
