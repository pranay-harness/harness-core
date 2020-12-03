package software.wings.delegatetasks.azure.appservice;

import static io.harness.azure.model.AzureConstants.COMMAND_TYPE_BLANK_VALIDATION_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.InvalidArgumentsException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AzureAppServiceTaskFactory {
  @Inject private Map<String, AbstractAzureAppServiceTaskHandler> azureAppServiceTaskTypeToTaskHandlerMap;

  public AbstractAzureAppServiceTaskHandler getAzureAppServiceTask(String commandType) {
    if (isBlank(commandType)) {
      throw new InvalidArgumentsException(COMMAND_TYPE_BLANK_VALIDATION_MSG);
    }
    return azureAppServiceTaskTypeToTaskHandlerMap.get(commandType);
  }
}
