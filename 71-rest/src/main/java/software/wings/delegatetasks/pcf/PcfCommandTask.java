package software.wings.delegatetasks.pcf;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
@Slf4j
public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

  public PcfCommandTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public PcfCommandExecutionResponse run(TaskParameters parameters) {
    if (!(parameters instanceof PcfRunPluginCommandRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfPluginCommandRequest"));
    }
    final PcfRunPluginCommandRequest pluginCommandRequest = (PcfRunPluginCommandRequest) parameters;
    return getPcfCommandExecutionResponse(pluginCommandRequest, pluginCommandRequest.getEncryptedDataDetails());
  }

  @Override
  public PcfCommandExecutionResponse run(Object[] parameters) {
    final PcfCommandRequest pcfCommandRequest = (PcfCommandRequest) parameters[0];
    final List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) parameters[1];
    return getPcfCommandExecutionResponse(pcfCommandRequest, encryptedDataDetails);
  }

  private PcfCommandExecutionResponse getPcfCommandExecutionResponse(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      return commandTaskTypeToTaskHandlerMap.get(pcfCommandRequest.getPcfCommandType().name())
          .executeTask(pcfCommandRequest, encryptedDataDetails);
    } catch (Exception ex) {
      logger.error(format("Exception in processing PCF task [%s]", pcfCommandRequest.toString()), ex);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}
