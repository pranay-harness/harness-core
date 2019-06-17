package software.wings.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuditPreferenceResponse {
  List<AuditPreference> auditPreferences;
  Map<String, ResourceLookup> resourceLookupMap;
}
