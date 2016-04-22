package software.wings.waitnotify;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.util.Objects;

/**
 * Represents WaitQueue.
 * @author Rishi
 */
@Entity(value = "waitQueues", noClassnameStored = true)
public class WaitQueue extends Base {
  @Indexed private String waitInstanceId;

  @Indexed private String correlationId;

  public WaitQueue() {}

  /**
   * creates a WaitQueue object.
   * @param waitInstanceId id of WaitInstance.
   * @param correlationId correlationId WaitInstance is waiting on.
   */
  public WaitQueue(String waitInstanceId, String correlationId) {
    super();
    this.waitInstanceId = waitInstanceId;
    this.correlationId = correlationId;
  }

  public String getWaitInstanceId() {
    return waitInstanceId;
  }

  public void setWaitInstanceId(String waitInstanceId) {
    this.waitInstanceId = waitInstanceId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("waitInstanceId", waitInstanceId)
        .add("correlationId", correlationId)
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    WaitQueue waitQueue = (WaitQueue) obj;
    return Objects.equals(waitInstanceId, waitQueue.waitInstanceId)
        && Objects.equals(correlationId, waitQueue.correlationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), waitInstanceId, correlationId);
  }
}
