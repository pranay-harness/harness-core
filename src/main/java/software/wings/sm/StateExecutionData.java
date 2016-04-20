/**
 *
 */
package software.wings.sm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rishi
 */
public class StateExecutionData implements Serializable {
  private static final long serialVersionUID = 1L;

  private long startTs;
  private long endTs;
  private ExecutionStatus status;
  private Map<String, ? extends Serializable> addlData = new HashMap<>();

  public long getStartTs() {
    return startTs;
  }

  public void setStartTs(long startTs) {
    this.startTs = startTs;
  }

  public long getEndTs() {
    return endTs;
  }

  public void setEndTs(long endTs) {
    this.endTs = endTs;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public Map<String, ? extends Serializable> getAddlData() {
    return addlData;
  }

  public void setAddlData(Map<String, ? extends Serializable> addlData) {
    this.addlData = addlData;
  }
}
