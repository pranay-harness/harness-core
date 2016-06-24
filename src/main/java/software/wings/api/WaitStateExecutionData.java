/**
 *
 */

package software.wings.api;

import software.wings.sm.StateExecutionData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The type Wait state execution data.
 *
 * @author Rishi
 */
public class WaitStateExecutionData extends StateExecutionData {
  private static final long serialVersionUID = 1L;
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
  public Object getExecutionSummary() {
    LinkedHashMap<String, Object> execData = fillExecutionData();
    execData.putAll((Map<String, Object>) super.getExecutionSummary());
    return execData;
  }

  @Override
  public Object getExecutionDetails() {
    LinkedHashMap<String, Object> execData = fillExecutionData();
    execData.putAll((Map<String, Object>) super.getExecutionSummary());
    return execData;
  }

  private LinkedHashMap<String, Object> fillExecutionData() {
    LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<>();
    putNotNull(orderedMap, "duration", duration);
    return orderedMap;
  }
}
