package software.wings.beans;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditPreferenceResponse {
  List<AuditPreference> auditPreferences;
  Map<String, ResourceLookup> resourceLookupMap;
}
