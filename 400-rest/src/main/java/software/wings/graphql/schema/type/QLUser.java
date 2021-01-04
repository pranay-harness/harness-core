package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserKeys")
@Scope(ResourceType.USER)
public class QLUser implements QLObject {
  private String id;
  private String name;
  private String email;
  private Boolean isEmailVerified;
  private Boolean isTwoFactorAuthenticationEnabled;
  private Boolean isUserLocked;
  private Boolean isPasswordExpired;
  private Boolean isImportedFromIdentityProvider;
}
