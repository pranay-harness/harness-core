/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import java.util.Optional;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public abstract class SlotStatusVerifier {
  private final String slotName;
  private final AzureWebClient azureWebClient;
  private final AzureServiceCallBack restCallBack;
  protected final LogCallback logCallback;
  protected final AzureWebClientContext azureWebClientContext;

  public enum SlotStatus { STOPPED, RUNNING }
  public enum SlotStatusVerifierType { STOP_VERIFIER, START_VERIFIER, SWAP_VERIFIER }

  public SlotStatusVerifier(LogCallback logCallback, String slotName, AzureWebClient azureWebClient,
      AzureWebClientContext azureWebClientContext, AzureServiceCallBack restCallBack) {
    this.logCallback = logCallback;
    this.slotName = slotName;
    this.azureWebClient = azureWebClient;
    this.azureWebClientContext = azureWebClientContext;
    this.restCallBack = restCallBack;
  }

  public boolean hasReachedSteadyState() {
    DeploymentSlot slot = getDeploymentSlot();
    String currentSlotState = slot.state();
    logCallback.saveExecutionLog(format("Current state for deployment slot is - [%s]", currentSlotState));
    return getSteadyState().equalsIgnoreCase(currentSlotState);
  }

  public boolean operationFailed() {
    return restCallBack.callFailed();
  }

  public String getErrorMessage() {
    return restCallBack.getErrorMessage();
  }

  public abstract String getSteadyState();

  protected DeploymentSlot getDeploymentSlot() {
    Optional<DeploymentSlot> deploymentSlotOptional =
        azureWebClient.getDeploymentSlotByName(azureWebClientContext, slotName);
    return deploymentSlotOptional.orElseThrow(
        () -> new InvalidRequestException(format("Unable to find deployment slot with name: %s", slotName)));
  }

  public static SlotStatusVerifier getStatusVerifier(String verifierType, LogCallback logCallback, String slotName,
      AzureWebClient azureWebClient, AzureMonitorClient azureMonitorClient, AzureWebClientContext azureWebClientContext,
      AzureServiceCallBack restCallBack) {
    switch (verifierType) {
      case "STOP_VERIFIER":
        return new StopSlotStatusVerifier(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
      case "START_VERIFIER":
        return new StartSlotStatusVerifier(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
      case "SWAP_VERIFIER":
        return new SwapSlotStatusVerifier(
            logCallback, slotName, azureWebClient, azureMonitorClient, azureWebClientContext, restCallBack);
      default:
        throw new InvalidRequestException(String.format("No slot status verifier defined for - [%s]", verifierType));
    }
  }
}
