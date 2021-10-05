package software.wings.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(HarnessTeam.CDC)
public class DiscoveryNode {
  private CgEntityNode entityNode;
  private Set<CgEntityId> children;
}
