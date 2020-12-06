package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class WebhookGitParam {
  private List<String> filePaths;
  private String gitConnectorId;
  private String branchName;
  private String repoName;
}
