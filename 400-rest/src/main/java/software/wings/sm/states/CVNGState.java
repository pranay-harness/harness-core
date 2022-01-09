/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.tasks.ResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@Data
@FieldNameConstants(innerTypeName = "CVNGStateKeys")
@Slf4j
@OwnedBy(CV)
public class CVNGState extends State {
  @VisibleForTesting static final String DEFAULT_HOSTNAME_TEMPLATE = "${instanceDetails.hostName}";
  private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMinutes(2);
  @SchemaIgnore @Inject private Clock clock;
  private String webhookUrl;
  private String deploymentTag;
  private String dataCollectionDelay;
  private String orgIdentifier;
  private String projectIdentifier;
  private String verificationJobIdentifier;
  private List<ParamValue> cvngParams;
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ParamValue {
    String name;
    String value;
    boolean editable;
  }

  public CVNGState(String name) {
    super(name, StateType.CVNG.name());
  }
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return ExecutionResponse.builder()
        .async(false)
        .executionStatus(
            ExecutionStatus
                .SKIPPED) // We should not have this configured anywhere but skipping it so that workflow does not fail.
        .errorMessage("CVNG integration is no longer supported. Please remove this step from your workflow.")
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext executionContext, Map<String, ResponseData> response) {
    throw new IllegalStateException(
        "CVNG integration is no longer supported. Please remove this step from your workflow.");
  }
  // keeping these classes to avoid any serialization issue for old executions. We can remove everything after few
  // months.
  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "CVNGStateExecutionDataKeys")
  public static class CVNGStateExecutionData extends StateExecutionData {
    private String activityId;
    private String projectIdentifier;
    private String orgIdentifier;
    private String deploymentTag;
    private String serviceIdentifier;
    private String stateExecutionInstanceId;
    private String envIdentifier;

    @Override
    public Map<String, ExecutionDataValue> getExecutionSummary() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
      setExecutionData(executionDetails);
      return executionDetails;
    }

    @Override
    public Map<String, ExecutionDataValue> getExecutionDetails() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
      setExecutionData(executionDetails);
      return executionDetails;
    }

    private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
      Map<String, String> idsMap = new HashMap<>();
      idsMap.put(CVNGStateExecutionDataKeys.activityId, activityId);
      idsMap.put(CVNGStateExecutionDataKeys.orgIdentifier, orgIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.projectIdentifier, projectIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.envIdentifier, envIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.serviceIdentifier, serviceIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.deploymentTag, deploymentTag);

      putNotNull(
          executionDetails, "cvngIds", ExecutionDataValue.builder().displayName("cvngIds").value(idsMap).build());
    }
  }

  @Value
  public static class CVNGStateResponseData implements ResponseData {
    private String activityId;
    private String correlationId;
    private ActivityStatusDTO activityStatusDTO;
    private String status;
    private ExecutionStatus executionStatus;
    @Builder
    public CVNGStateResponseData(String activityId, String correlationId, ActivityStatusDTO activityStatusDTO,
        String status, ExecutionStatus executionStatus) {
      this.activityId = activityId;
      this.correlationId = correlationId;
      this.activityStatusDTO = activityStatusDTO;
      this.status = status;
      if (executionStatus == null) {
        this.executionStatus = setExecutionUsingStatus();
      } else {
        this.executionStatus = executionStatus;
      }
    }

    private ExecutionStatus setExecutionUsingStatus() {
      switch (activityStatusDTO.getStatus()) {
        case VERIFICATION_PASSED:
          return ExecutionStatus.SUCCESS;
        case IN_PROGRESS:
        case NOT_STARTED:
          return ExecutionStatus.RUNNING;
        case VERIFICATION_FAILED:
          return ExecutionStatus.FAILED;
        case ERROR:
          return ExecutionStatus.ERROR;
        default:
          throw new IllegalStateException("Unhandled status value: " + activityStatusDTO.getStatus());
      }
    }
  }
}
