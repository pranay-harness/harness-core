package software.wings.security.authentication.totp;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface NotificationService {
  /**
   *
   * @param email
   */
  void notifyUser(String email);

  /**
   *
   * @param userUuid
   */
  void notifySecOps(String userUuid);
}
