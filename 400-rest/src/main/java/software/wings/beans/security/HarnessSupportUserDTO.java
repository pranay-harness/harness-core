package software.wings.beans.security;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class HarnessSupportUserDTO {
  private String name;
  private String id;
  private String emailId;
}
