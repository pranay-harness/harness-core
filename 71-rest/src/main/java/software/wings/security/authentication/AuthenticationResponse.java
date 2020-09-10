package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.beans.User;

@OwnedBy(PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class AuthenticationResponse {
  User user;
}
