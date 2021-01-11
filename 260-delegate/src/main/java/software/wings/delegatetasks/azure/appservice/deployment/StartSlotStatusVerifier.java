package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

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
