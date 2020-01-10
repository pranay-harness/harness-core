package software.wings.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiKeyAuditDetails {
  private String apiKeyId;
}
