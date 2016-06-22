/**
 *
 */

package software.wings.api;

import software.wings.sm.StateExecutionData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rishi
 */
public class WaitStateExecutionData extends StateExecutionData {
  private static final long serialVersionUID = 1L;
  private long duration;
  private long wakeupTs;
  private String resumeId;

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getWakeupTs() {
    return wakeupTs;
  }

  public void setWakeupTs(long wakeupTs) {
    this.wakeupTs = wakeupTs;
  }

  public String getResumeId() {
    return resumeId;
  }

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
