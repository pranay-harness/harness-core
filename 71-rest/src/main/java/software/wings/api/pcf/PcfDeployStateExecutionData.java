package software.wings.api.pcf;

import com.google.common.collect.Maps;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcfDeployStateExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private String releaseName;
  private String commandName;
  private List<PcfServiceData> instanceData;
  private String updateDetails;
  private PcfSetupContextElement setupContextElement;

  private Integer updateCount;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();
    putNotNull(executionDetails, "commandName",
        ExecutionDataValue.builder().value(commandName).displayName("CommandName").build());
    putNotNull(executionDetails, "updateDetails",
        ExecutionDataValue.builder().value(updateDetails).displayName("Resize Details").build());

    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    return PcfDeployExecutionSummary.builder().releaseName(releaseName).instaceData(instanceData).build();
  }
}
