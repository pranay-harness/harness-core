package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WebhookGitParam {
  private List<String> filePaths;
  private String gitConnectorId;
  private String branchName;
}
