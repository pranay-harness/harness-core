/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.cf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.pcf.CfDeploymentManager;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDP)
public abstract class PcfCommandTaskHandler {
  @Inject protected CfDeploymentManager pcfDeploymentManager;
  @Inject protected SecretDecryptionService secretDecryptionService;
  @Inject protected PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper;

  public CfCommandExecutionResponse executeTask(CfCommandRequest cfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, boolean isInstanceSync,
      ILogStreamingTaskClient logStreamingTaskClient) {
    return executeTaskInternal(cfCommandRequest, encryptedDataDetails, logStreamingTaskClient, isInstanceSync);
  }

  protected abstract CfCommandExecutionResponse executeTaskInternal(CfCommandRequest cfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ILogStreamingTaskClient logStreamingTaskClient,
      boolean isInstanceSync);
}
