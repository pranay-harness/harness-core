package software.wings.yaml.gitSync;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GitFileProcessingSummary {
  private long failureCount;
  private long successCount;
  private long totalCount;
  private long skippedCount;
  private long originalCount;
  private long otherCount;
}
