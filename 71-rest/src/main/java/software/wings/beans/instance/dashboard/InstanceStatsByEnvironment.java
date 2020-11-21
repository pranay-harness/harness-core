package software.wings.beans.instance.dashboard;

import software.wings.beans.infrastructure.instance.SyncStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/13/17
 */
@Data
@Builder
public class InstanceStatsByEnvironment {
  private EnvironmentSummary environmentSummary;
  private List<InstanceStatsByArtifact> instanceStatsByArtifactList;
  private List<SyncStatus> infraMappingSyncStatusList;
  private boolean hasSyncIssues;
}
