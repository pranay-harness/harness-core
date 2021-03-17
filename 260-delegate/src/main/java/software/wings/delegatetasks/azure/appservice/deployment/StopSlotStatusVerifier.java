package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

@TargetModule(Module._930_DELEGATE_TASKS)
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
