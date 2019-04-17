package software.wings.api;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ServiceNowExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private ExecutionStatus executionStatus;
  private String issueUrl;
  private String message;
  private String issueId;
  private String issueNumber;
  private String responseMsg;
  private ServiceNowTicketType ticketType;

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
    return executionDetails;
  }
}
