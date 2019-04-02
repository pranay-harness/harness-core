package software.wings.delegatetasks;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTaskResponse;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CollaborationProviderTask extends AbstractDelegateRunnableTask {
  private static Logger logger = LoggerFactory.getLogger(CollaborationProviderTask.class);
  @Inject EmailHandler emailHandler;

  public CollaborationProviderTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(Object[] parameters) {
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
