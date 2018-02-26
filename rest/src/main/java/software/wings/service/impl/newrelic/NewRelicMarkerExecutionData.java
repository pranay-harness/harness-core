package software.wings.service.impl.newrelic;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class NewRelicMarkerExecutionData extends StateExecutionData {
  private String payload;
  private String evaluatedPayload;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "errorMsg",
        anExecutionDataValue().withValue(getErrorMsg()).withDisplayName("Message").build());
    putNotNull(
        executionDetails, "payload", anExecutionDataValue().withDisplayName("Payload").withValue(payload).build());

    putNotNull(executionDetails, "evaluatedPayload",
        anExecutionDataValue().withDisplayName("Evaluated Payload").withValue(evaluatedPayload).build());

    return executionDetails;
  }
}
