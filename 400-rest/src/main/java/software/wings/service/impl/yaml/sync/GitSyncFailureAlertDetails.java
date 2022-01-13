/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
