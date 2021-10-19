package software.wings.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NGMigrationStatus {
  // Can we migrate the given entity or not
  private boolean status;
  // List of reasons why we cannot the entity. Note: Empty if status is True
  private List<String> reasons;
}
