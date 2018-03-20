package software.wings.service.impl.analysis;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 8/4/16.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class LogAnalysisExecutionData extends StateExecutionData {
  private String correlationId;
  private String stateExecutionInstanceId;
  private String serverConfigId;
  private String query;
  private int timeDuration;
  private Set<String> canaryNewHostNames;
  private Set<String> lastExecutionNodes;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, "stateExecutionInstanceId",
        anExecutionDataValue().withValue(stateExecutionInstanceId).withDisplayName("State Execution Id").build());
    putNotNull(executionDetails, "serverConfigId",
        anExecutionDataValue().withValue(serverConfigId).withDisplayName("Server Config Id").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "errorMsg",
        anExecutionDataValue().withValue(getErrorMsg()).withDisplayName("Message").build());
    final int total = timeDuration + SplunkDataCollectionTask.DELAY_MINUTES + 1;
    putNotNull(executionDetails, "total", anExecutionDataValue().withDisplayName("Total").withValue(total).build());
    int elapsedMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - getStartTs());
    if (elapsedMinutes < SplunkDataCollectionTask.DELAY_MINUTES + 1) {
      elapsedMinutes = 0;
    } else {
      elapsedMinutes = elapsedMinutes - (SplunkDataCollectionTask.DELAY_MINUTES + 1);
    }
    final CountsByStatuses breakdown = new CountsByStatuses();
    switch (getStatus()) {
      case FAILED:
        breakdown.setFailed(total);
        break;
      case SUCCESS:
        breakdown.setSuccess(total);
        break;
      default:
        breakdown.setSuccess(Math.min(elapsedMinutes, total));
        break;
    }
    putNotNull(executionDetails, "breakdown",
        anExecutionDataValue().withDisplayName("breakdown").withValue(breakdown).build());
    putNotNull(executionDetails, "timeDuration",
        anExecutionDataValue().withValue(timeDuration).withDisplayName("Analysis duration").build());
    putNotNull(executionDetails, "queries", anExecutionDataValue().withValue(query).withDisplayName("Queries").build());
    return executionDetails;
  }
}
