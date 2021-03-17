package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class NGLoginResponse {
  private boolean showCaptcha;
  private User user;
  private DefaultExperience defaultExperience;
}
