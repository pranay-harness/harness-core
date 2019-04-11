package software.wings.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.EntityYamlRecord;

import java.util.List;

@Value
@Builder
public class AuditHeaderYamlResponse {
  private String auditHeaderId;
  private List<EntityYamlRecord> entityAuditYamls;
}
