package software.wings.delegatetasks;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.exception.HarnessException;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private PcfDeploymentManager pcfDeploymentManager;
  @Inject private EncryptionService encryptionService;
  @Inject private PcfCommandTaskHelper pcfCommandTaskHelper;
  @Inject private DelegateFileManager delegateFileManager;

  private static final Logger logger = LoggerFactory.getLogger(PcfCommandTask.class);

  public PcfCommandTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public PcfCommandExecutionResponse run(Object[] parameters) {
    PcfCommandRequest pcfCommandRequest = (PcfCommandRequest) parameters[0];

    ExecutionLogCallback executionLogCallback =
        new ExecutionLogCallback(delegateLogService, pcfCommandRequest.getAccountId(), pcfCommandRequest.getAppId(),
            pcfCommandRequest.getActivityId(), pcfCommandRequest.getCommandName());

    List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) parameters[1];
    try {
      switch (pcfCommandRequest.getPcfCommandType()) {
        case SETUP:
          return pcfCommandTaskHelper.performSetup(pcfCommandRequest, executionLogCallback, encryptionService,
              pcfDeploymentManager, delegateFileManager, encryptedDataDetails);
        case RESIZE:
          return pcfCommandTaskHelper.performDeploy(
              pcfCommandRequest, executionLogCallback, encryptionService, pcfDeploymentManager, encryptedDataDetails);
        case ROLLBACK:
          return pcfCommandTaskHelper.performDeployRollback(
              pcfCommandRequest, executionLogCallback, encryptionService, pcfDeploymentManager, encryptedDataDetails);
        case DATAFETCH:
          return pcfCommandTaskHelper.performDataFetch(
              pcfCommandRequest, executionLogCallback, encryptionService, pcfDeploymentManager, encryptedDataDetails);
        case VALIDATE:
          return pcfCommandTaskHelper.performValidation(pcfCommandRequest, executionLogCallback, pcfDeploymentManager);
        default:
          throw new HarnessException("Operation not supported");
      }
    } catch (Exception ex) {
      logger.error("Exception in processing PCF task [{}]", pcfCommandRequest, ex);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }
  }
}
