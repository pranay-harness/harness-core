/**
 *
 */

package software.wings.api;

import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * The type Wait state execution data.
 *
 * @author Rishi
 */
public class WaitStateExecutionData extends StateExecutionData {
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
