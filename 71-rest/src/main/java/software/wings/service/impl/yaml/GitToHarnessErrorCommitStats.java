package software.wings.service.impl.yaml;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.List;

@Data
@FieldNameConstants(innerTypeName = "GitToHarnessErrorCommitStatsKeys")
public class GitToHarnessErrorCommitStats {
  String gitCommitId;
  Integer failedCount;
  Long commitTime;
  String gitConnectorId;
  String branchName;
  String gitConnectorName;
  List<GitSyncError> errorsForSummaryView;
}
