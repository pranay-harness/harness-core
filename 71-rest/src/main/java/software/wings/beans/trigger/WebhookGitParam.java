package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class WebhookGitParam {
  private List<String> filePaths;
  private String gitConnectorId;
  private String branchName;
}
