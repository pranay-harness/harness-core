/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
