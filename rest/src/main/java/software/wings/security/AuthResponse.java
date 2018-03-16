package software.wings.security;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 02/12/18
 */

@Data
@EqualsAndHashCode
@Builder
public class AuthResponse {
  public enum Status { SUCCESS, FAILURE }

  private Status status;
  private String message;
}
