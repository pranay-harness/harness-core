package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateEncryptedTextInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLEncryptedTextInput {
  private String secretManagerId;
  private String name;
  private String value;
  private String clientMutationId;
}
