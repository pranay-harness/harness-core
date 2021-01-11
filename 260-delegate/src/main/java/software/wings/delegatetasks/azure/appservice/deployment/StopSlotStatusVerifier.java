package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

public class StopSlotStatusVerifier extends SlotStatusVerifier {
  public StopSlotStatusVerifier(LogCallback logCallback, String slotName, AzureWebClient azureWebClient,
      AzureWebClientContext azureWebClientContext, AzureServiceCallBack restCallBack) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
  }

  @Override
  public String getSteadyState() {
    return SlotStatus.STOPPED.name();
  }
}
