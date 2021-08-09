package software.wings.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitAuditDetails {
  private String author;
  private String gitCommitId;
  private String repoUrl;
}
