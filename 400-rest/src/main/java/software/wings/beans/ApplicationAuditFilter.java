package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ApplicationAuditFilter {
  private List<String> appIds;
  private List<String> resourceTypes;
  private List<String> resourceIds;
}
