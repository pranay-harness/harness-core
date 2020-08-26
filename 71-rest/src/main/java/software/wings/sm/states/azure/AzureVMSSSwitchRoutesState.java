package software.wings.sm.states.azure;

import static io.harness.exception.ExceptionUtils.getMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureVMSSSwitchRoutesState extends State {
  @Getter @Setter private boolean downsizeOldVMSSS;
  public AzureVMSSSwitchRoutesState(String name) {
    super(name, StateType.AZURE_VMSS_SWITCH_ROUTES.name());
  }

  public AzureVMSSSwitchRoutesState(String name, String stateType) {
    super(name, stateType);
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
