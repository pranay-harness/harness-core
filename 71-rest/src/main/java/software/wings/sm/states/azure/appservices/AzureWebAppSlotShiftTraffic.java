package software.wings.sm.states.azure.appservices;

import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SHIFT_TRAFFIC;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.tasks.ResponseData;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotShiftTraffic extends State {
  @Getter @Setter private String trafficWeightExpr;
  @Getter @Setter private boolean deleteOldSlot;
  public static final String APP_SERVICE_SLOT_TRAFFIC_SHIT = "App Service Slot Traffic Shift";

  public AzureWebAppSlotShiftTraffic(String name) {
    super(name, AZURE_WEBAPP_SLOT_SHIFT_TRAFFIC.name());
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
}
