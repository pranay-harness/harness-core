package io.harness.delegate.task;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.MicrosoftTeamsTaskParams;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.service.senders.MSTeamsSenderImpl;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class MicrosoftTeamsSenderDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private MSTeamsSenderImpl microsoftTeamsSender;

  public MicrosoftTeamsSenderDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    MicrosoftTeamsTaskParams microsoftTeamsTaskParams = (MicrosoftTeamsTaskParams) parameters;
    try {
      NotificationProcessingResponse processingResponse =
          microsoftTeamsSender.send(microsoftTeamsTaskParams.getMicrosoftTeamsWebhookUrls(),
              microsoftTeamsTaskParams.getMessage(), microsoftTeamsTaskParams.getNotificationId());
      return NotificationTaskResponse.builder().processingResponse(processingResponse).build();
    } catch (Exception e) {
      return NotificationTaskResponse.builder()
          .processingResponse(NotificationProcessingResponse.trivialResponseWithNoRetries)
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
