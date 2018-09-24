package software.wings.delegatetasks;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CollaborationProviderTask extends AbstractDelegateRunnableTask {
  private static Logger logger = LoggerFactory.getLogger(CollaborationProviderTask.class);
  @Inject EmailHandler emailHandler;

  public CollaborationProviderTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public NotifyResponseData run(Object[] parameters) {
    CollaborationProviderRequest request = (CollaborationProviderRequest) parameters[0];
    try {
      switch (request.getCommunicationType()) {
        case EMAIL:
          return emailHandler.handle(request);
        default:
          throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
      }
    } catch (Exception e) {
      logger.error(format("Exception in processing externalCommunicationTask task [%s]", request.toString()), e);
      return CollaborationProviderResponse.builder()
          .status(CommandExecutionStatus.FAILURE)
          .accountId(getAccountId())
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
