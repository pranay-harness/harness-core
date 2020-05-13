package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ServiceNowExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private ExecutionStatus executionStatus;
  private String issueUrl;
  private String message;
  private String issueId;
  private String issueNumber;
  private String responseMsg;
  private ServiceNowTicketType ticketType;

  // import set field
  private ServiceNowImportSetResponse transformationDetails;
  private List<String> transformationValues;

  // approvalField
  private String currentState;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    return setExecutionData(executionDetails);
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    return setExecutionData(executionDetails);
  }

  private Map<String, ExecutionDataValue> setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    if (ticketType != null && issueUrl != null) {
      putNotNull(executionDetails, "issueUrl",
          ExecutionDataValue.builder().displayName(ticketType.getDisplayName() + " Url").value(issueUrl).build());
    }

    if (EmptyPredicate.isNotEmpty(transformationValues)) {
      putNotNull(executionDetails, "transformationValues",
          ExecutionDataValue.builder()
              .displayName("Transformation Values")
              .value(transformationValues.toString().replaceAll("[\\[\\]]", ""))
              .build());
    }

    if (currentState != null) {
      putNotNull(executionDetails, "currentState",
          ExecutionDataValue.builder().displayName("current state").value(currentState).build());
    }
    return executionDetails;
  }
}
