package software.wings.helpers.ext.ldap;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapResponse {
  public enum Status { SUCCESS, FAILURE }

  Status status;
  String message;
}
