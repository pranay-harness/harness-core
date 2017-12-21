package software.wings.api;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.common.Constants;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SplunkStateExecutionData extends StateExecutionData {
  private String query;
  private String response;
  private String assertionStatement;
  private String assertionStatus;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "query", anExecutionDataValue().withDisplayName("Query").withValue(query).build());
    putNotNull(
        executionDetails, "response", anExecutionDataValue().withDisplayName("Response").withValue(response).build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withDisplayName("Assertion").withValue(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withDisplayName("Assertion Result").withValue(assertionStatus).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "query", anExecutionDataValue().withDisplayName("Query").withValue(query).build());
    putNotNull(executionDetails, "response",
        anExecutionDataValue()
            .withDisplayName("Response")
            .withValue(abbreviate(response, Constants.SUMMARY_PAYLOAD_LIMIT))
            .build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withDisplayName("Assertion").withValue(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withDisplayName("Assertion Result").withValue(assertionStatus).build());
    return executionDetails;
  }
}
