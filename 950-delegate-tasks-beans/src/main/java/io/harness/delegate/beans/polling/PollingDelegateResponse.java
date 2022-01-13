/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class PollingDelegateResponse {
  private String accountId;
  private String pollingDocId;
  private PollingResponseInfc pollingResponseInfc;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
