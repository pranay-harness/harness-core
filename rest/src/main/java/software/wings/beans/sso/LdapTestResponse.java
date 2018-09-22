package software.wings.beans.sso;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class LdapTestResponse {
  public enum Status { SUCCESS, FAILURE }

  @NotBlank Status status;
  String message;
}
