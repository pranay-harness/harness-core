package software.wings.sm.states.azure;

import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.sm.StateType.AZURE_VMSS_DEPLOY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InstanceUnitType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureVMSSDeployState extends State {
  @Getter @Setter private InstanceUnitType instanceUnitType = PERCENTAGE;
  @Getter @Setter private String instanceCountExpr = "100";

  public AzureVMSSDeployState(String name) {
    super(name, AZURE_VMSS_DEPLOY.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
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
}
