package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;

/**
 * Created by rishi on 4/3/17.
 */
public class BarrierExecutionData extends StateExecutionData {
  private String identifier;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "identifier",
        anExecutionDataValue().withDisplayName("Identifier").withValue(identifier).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "identifier",
        anExecutionDataValue().withDisplayName("Identifier").withValue(identifier).build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    BarrierStepExecutionSummary barrierStepExecutionSummary = new BarrierStepExecutionSummary();
    populateStepExecutionSummary(barrierStepExecutionSummary);
    barrierStepExecutionSummary.setIdentifier(identifier);
    return barrierStepExecutionSummary;
  }
}
