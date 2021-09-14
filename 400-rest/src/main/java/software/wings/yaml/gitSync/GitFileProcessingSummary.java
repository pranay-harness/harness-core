/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
