package io.harness.gitsync.gitfileactivity.beans;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@Document("gitFileProcessingSummaryNG")
@TypeAlias("io.harness.gitsync.gitfileactivity.beans.gitFileProcessingSummary")
public class GitFileProcessingSummary {
  private final Long failureCount;
  private final Long successCount;
  private final Long totalCount;
  private final Long skippedCount;
  private final Long queuedCount;
}
