package software.wings.delegatetasks.azure.appservice.deployment;

import static java.lang.String.format;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

public class SlotSwapper implements Runnable {
  private final String sourceSlotName;
  private final String targetSlotName;
  private final AzureWebClient azureWebClient;
  private final AzureWebClientContext webClientContext;
  private final AzureServiceCallBack callBack;
  private final LogCallback slotSwapLogCallback;

  public SlotSwapper(String sourceSlotName, String targetSlotName, AzureWebClient azureWebClient,
      AzureWebClientContext webClientContext, AzureServiceCallBack callBack, LogCallback slotSwapLogCallback) {
    this.sourceSlotName = sourceSlotName;
    this.targetSlotName = targetSlotName;
    this.azureWebClient = azureWebClient;
    this.webClientContext = webClientContext;
    this.callBack = callBack;
    this.slotSwapLogCallback = slotSwapLogCallback;
  }

  @Override
  public void run() {
    slotSwapLogCallback.saveExecutionLog(format(
        "Sending request for swapping source slot: [%s] with target slot: [%s]", sourceSlotName, targetSlotName));
    azureWebClient.swapDeploymentSlotsAsync(webClientContext, sourceSlotName, targetSlotName, callBack);
    slotSwapLogCallback.saveExecutionLog("Swapping request returned successfully");
  }
}
