/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.sdk.core.data.Outcome;

import software.wings.sm.StateExecutionData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;

/**
 * The type Wait state execution data.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@JsonTypeName("waitStateExecutionData")
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WaitStateExecutionData extends StateExecutionData implements Outcome {
  private long duration;
  private long wakeupTs;
  private String resumeId;

  /**
   * Gets duration.
   *
   * @return the duration
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Sets duration.
   *
   * @param duration the duration
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  /**
   * Gets wakeup ts.
   *
   * @return the wakeup ts
   */
  public long getWakeupTs() {
    return wakeupTs;
  }

  /**
   * Sets wakeup ts.
   *
   * @param wakeupTs the wakeup ts
   */
  public void setWakeupTs(long wakeupTs) {
    this.wakeupTs = wakeupTs;
  }

  /**
   * Gets resume id.
   *
   * @return the resume id
   */
  public String getResumeId() {
    return resumeId;
  }

  /**
   * Sets resume id.
   *
   * @param resumeId the resume id
   */
  public void setResumeId(String resumeId) {
    this.resumeId = resumeId;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "duration",
        ExecutionDataValue.builder().displayName("Duration (In Seconds)").value(duration).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "duration",
        ExecutionDataValue.builder().displayName("Duration (In Seconds)").value(duration).build());
    return executionDetails;
  }
}
