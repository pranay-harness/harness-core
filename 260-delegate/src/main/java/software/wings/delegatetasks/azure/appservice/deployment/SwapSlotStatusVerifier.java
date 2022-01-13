/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

import com.microsoft.azure.management.monitor.EventData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class SwapSlotStatusVerifier extends SlotStatusVerifier {
  private final DateTime startTime;
  private final AzureMonitorClient azureMonitorClient;
  private final Set<SwapSlotStatus> seenBefore = new HashSet<>();
  private SwapSlotStatus previousHighestStatus = SwapSlotStatus.NOT_STARTED;

  public SwapSlotStatusVerifier(LogCallback logCallback, String slotName, AzureWebClient azureWebClient,
      AzureMonitorClient azureMonitorClient, AzureWebClientContext azureWebClientContext,
      AzureServiceCallBack restCallBack) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
    startTime = DateTime.now().minusMinutes(1);
    this.azureMonitorClient = azureMonitorClient;
  }

  @Override
  public boolean hasReachedSteadyState() {
    String slotId = getDeploymentSlot().id();
    AzureConfig azureConfig = azureWebClientContext.getAzureConfig();
    String subscriptionId = azureWebClientContext.getSubscriptionId();
    List<EventData> eventData = azureMonitorClient.listEventDataWithAllPropertiesByResourceId(
        azureConfig, subscriptionId, startTime, DateTime.now(), slotId);

    if (eventData.isEmpty()) {
      return previousHighestStatus == SwapSlotStatus.SLOT_SWAP_ACTION;
    }

    // the oldest event is at the end
    for (int index = eventData.size() - 1; index >= 0; index--) {
      EventData event = eventData.get(index);
      if (AzureResourceUtility.isSlotSwapJobProcessor(event)) {
        String action = event.operationName().localizedValue();
        SwapSlotStatus currentStatus = SwapSlotStatus.getSlotStatus(action);
        if (SwapSlotStatus.compare(currentStatus, previousHighestStatus) || !seenBefore.contains(currentStatus)) {
          logCallback.saveExecutionLog(String.format("Operation name : [%s]%nStatus : [%s]%nDescription : [%s]", action,
              event.status().localizedValue(), event.description()));
          previousHighestStatus = currentStatus;
          seenBefore.add(currentStatus);
        }
      }
    }
    return previousHighestStatus == SwapSlotStatus.SLOT_SWAP_ACTION;
  }

  @Override
  public String getSteadyState() {
    return "Running";
  }

  private enum SwapSlotStatus {
    NOT_STARTED(""),
    APPLY_SLOT_CONFIGURATION("Microsoft.Web/sites/slots/ApplySlotConfig/action"),
    START_SLOT_WARM_UP("Microsoft.Web/sites/slots/StartSlotWarmup/action"),
    END_SLOT_WARM_UP("Microsoft.Web/sites/slots/EndSlotWarmup/action"),
    SLOT_SWAP_ACTION("Microsoft.Web/sites/slots/SlotSwap/action");

    private final String action;
    SwapSlotStatus(String action) {
      this.action = action;
    }

    public static boolean compare(SwapSlotStatus status1, SwapSlotStatus status2) {
      return status1.ordinal() >= status2.ordinal();
    }

    public static SwapSlotStatus getSlotStatus(String action) {
      for (SwapSlotStatus status : values()) {
        if (status.action.contains(action)) {
          return status;
        }
      }
      return NOT_STARTED;
    }
  }
}
