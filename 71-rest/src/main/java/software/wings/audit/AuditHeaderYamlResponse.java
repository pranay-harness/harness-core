package software.wings.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditHeaderYamlResponse {
  private String auditHeaderId;
  private String entityId;
  private String oldYaml;
  private String newYaml;
}