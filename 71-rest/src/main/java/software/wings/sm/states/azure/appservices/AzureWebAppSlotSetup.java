package software.wings.sm.states.azure.appservices;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SETUP;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.tasks.ResponseData;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotSetup extends State {
  @Getter @Setter private String newSlotName;
  @Getter @Setter private String slotSteadyStateTimeout;
  @Getter @Setter private List<AzureAppServiceApplicationSetting> applicationSettings;
  @Getter @Setter private List<AzureAppServiceConnectionString> appServiceConnectionStrings;

  public AzureWebAppSlotSetup(String name) {
    super(name, AZURE_WEBAPP_SLOT_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    throw new InvalidRequestException("Not implemented yet");
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    throw new InvalidRequestException("Not implemented yet");
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(newSlotName)) {
      invalidFields.put("SLot name", "Slot name must be specified");
    }
    return invalidFields;
  }
}
