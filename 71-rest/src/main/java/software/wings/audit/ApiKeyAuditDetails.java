package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ApiKeyAuditDetails {
  private String apiKeyId;
}
