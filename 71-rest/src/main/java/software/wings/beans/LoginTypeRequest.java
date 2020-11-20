package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class LoginTypeRequest {
  private String userName;
}
