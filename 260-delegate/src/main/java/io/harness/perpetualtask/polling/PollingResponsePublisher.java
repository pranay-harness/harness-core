package io.harness.perpetualtask.polling;

import static io.harness.network.SafeHttpCall.executeWithExceptions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.polling.PollingDelegateResponse;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class PollingResponsePublisher {
  private final KryoSerializer kryoSerializer;
  private final DelegateAgentManagerClient delegateAgentManagerClient;

  @Inject
  public PollingResponsePublisher(
      KryoSerializer kryoSerializer, DelegateAgentManagerClient delegateAgentManagerClient) {
    this.kryoSerializer = kryoSerializer;
    this.delegateAgentManagerClient = delegateAgentManagerClient;
  }

  public boolean publishToManger(String taskId, PollingDelegateResponse pollingDelegateResponse) {
    try {
      byte[] responseSerialized = kryoSerializer.asBytes(pollingDelegateResponse);

      executeWithExceptions(
          delegateAgentManagerClient.publishPollingResult(taskId, pollingDelegateResponse.getAccountId(),
              RequestBody.create(MediaType.parse("application/octet-stream"), responseSerialized)));
      return true;
    } catch (Exception ex) {
      log.error("Failed to publish polling response with status: {}",
          pollingDelegateResponse.getCommandExecutionStatus().name(), ex);
      return false;
    }
  }
}
