package software.wings.yaml.gitSync;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GitFileProcessingSummary {
  // count of files not processed successfully
  private Long failureCount;
  // count of files successfully processed
  private Long successCount;
  // count of files in the git diff
  private Long totalCount;
  // count of files skipped for processing
  private Long skippedCount;
  // count of file still undergoing processing
  private Long queuedCount;
}
