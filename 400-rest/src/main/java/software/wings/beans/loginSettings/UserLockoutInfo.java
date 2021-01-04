package software.wings.beans.loginSettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLockoutInfo {
  private int numberOfFailedLoginAttempts;
  @Builder.Default private long userLockedAt = 1L;
}
