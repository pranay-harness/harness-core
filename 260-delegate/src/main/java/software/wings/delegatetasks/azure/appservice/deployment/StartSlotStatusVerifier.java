/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class StartSlotStatusVerifier extends SlotStatusVerifier {
  public StartSlotStatusVerifier(LogCallback logCallback, String slotName, AzureWebClient azureWebClient,
      AzureWebClientContext azureWebClientContext, AzureServiceCallBack restCallBack) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
  }

  @Override
  public String getSteadyState() {
    return SlotStatus.RUNNING.name();
  }
}
