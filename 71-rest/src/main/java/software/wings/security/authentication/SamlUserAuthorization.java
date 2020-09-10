package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@OwnedBy(PL)
@Data
@Builder
public class SamlUserAuthorization {
  private String email;
  private List<String> userGroups;
}
