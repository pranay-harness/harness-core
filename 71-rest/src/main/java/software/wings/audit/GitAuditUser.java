package software.wings.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitAuditUser {
  private String author;
  private String gitCommitId;
}
