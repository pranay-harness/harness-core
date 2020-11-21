package software.wings.sm.states.azure.appservices;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SWAP;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.tasks.ResponseData;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotSwap extends State {
  @Getter @Setter private String subscriptionId;
  @Getter @Setter private String resourceGroup;
  @Getter @Setter private String webApp;
  @Getter @Setter private String targetSlot;
  public static final String APP_SERVICE_SLOT_SWAP = "App Service Slot Swap";

  public AzureWebAppSlotSwap(String name) {
    super(name, AZURE_WEBAPP_SLOT_SWAP.name());
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
    if (isEmpty(subscriptionId)) {
      invalidFields.put("Subscription Id", "Subscription Id must be specified");
    }
    if (isEmpty(resourceGroup)) {
      invalidFields.put("Resource Group", "Resource Group name must be specified");
    }
    if (isEmpty(webApp)) {
      invalidFields.put("Web App", "Web App name must be specified");
    }
    if (isEmpty(targetSlot)) {
      invalidFields.put("Target Slot", "Target Slot name must be specified");
    }
    return invalidFields;
  }
}
