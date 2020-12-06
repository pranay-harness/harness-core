package software.wings.delegatetasks;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;

import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class CollaborationProviderTask extends AbstractDelegateRunnableTask {
  @Inject EmailHandler emailHandler;

  public CollaborationProviderTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    CollaborationProviderRequest request = (CollaborationProviderRequest) parameters[0];
    try {
      switch (request.getCommunicationType()) {
        case EMAIL:
          return emailHandler.handle(request);
        default:
          throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
      }
    } catch (Exception e) {
      log.error("Exception in processing externalCommunicationTask task [{}]", request.toString(), e);
      return CollaborationProviderResponse.builder()
          .status(CommandExecutionStatus.FAILURE)
          .accountId(getAccountId())
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
