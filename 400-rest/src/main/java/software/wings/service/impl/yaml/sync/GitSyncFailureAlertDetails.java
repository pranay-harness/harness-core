/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.sync;

import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncFailureAlertDetails {
  private String errorMessage;
  private ErrorCode errorCode;
  private String gitConnectorId;
  private String branchName;
  private String repositoryName;
}
