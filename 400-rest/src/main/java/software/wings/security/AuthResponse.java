package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 02/12/18
 */

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode
public class AuthResponse {
  public enum Status { SUCCESS, FAILURE }

  private Status status;
  private String message;
}
