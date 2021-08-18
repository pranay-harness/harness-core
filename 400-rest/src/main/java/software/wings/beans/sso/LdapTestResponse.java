package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class LdapTestResponse {
  public enum Status { SUCCESS, FAILURE }

  @NotBlank Status status;
  String message;
}
