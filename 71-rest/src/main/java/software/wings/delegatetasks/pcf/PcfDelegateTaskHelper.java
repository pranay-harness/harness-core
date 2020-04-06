package software.wings.delegatetasks.pcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;

import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class PcfDelegateTaskHelper {
  @Inject private Map<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

  public PcfCommandExecutionResponse getPcfCommandExecutionResponse(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      return commandTaskTypeToTaskHandlerMap.get(pcfCommandRequest.getPcfCommandType().name())
          .executeTask(pcfCommandRequest, encryptedDataDetails);
    } catch (Exception ex) {
      logger.error("Exception in processing PCF task [{}]", pcfCommandRequest.toString(), ex);
      return PcfCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}
