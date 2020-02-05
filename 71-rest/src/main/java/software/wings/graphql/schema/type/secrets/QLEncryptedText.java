package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedTextKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLEncryptedText implements QLSecret {
  private String secretManagerId;
  private String name;
  private String id;
}
