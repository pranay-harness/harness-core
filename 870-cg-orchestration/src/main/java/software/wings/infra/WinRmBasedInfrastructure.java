package software.wings.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface WinRmBasedInfrastructure {
  String getWinRmConnectionAttributes();
}
