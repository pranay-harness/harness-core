package software.wings.waitnotify;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.util.Objects;

/**
 * Represents WaitQueue.
 *
 * @author Rishi
 */
@Entity(value = "waitQueues", noClassnameStored = true)
public class WaitQueue extends Base {
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String WAIT_INSTANCE_ID_KEY = "waitInstanceId";

  @Indexed private String waitInstanceId;

  @Indexed private String correlationId;

  /**
   * Instantiates a new wait queue.
   */
  public WaitQueue() {}

  /**
   * creates a WaitQueue object.
   *
   * @param waitInstanceId id of WaitInstance.
   * @param correlationId  correlationId WaitInstance is waiting on.
   */
  public WaitQueue(String waitInstanceId, String correlationId) {
    this.waitInstanceId = waitInstanceId;
    this.correlationId = correlationId;
  }

  /**
   * Gets wait instance id.
   *
   * @return the wait instance id
   */
  public String getWaitInstanceId() {
    return waitInstanceId;
  }

  /**
   * Sets wait instance id.
   *
   * @param waitInstanceId the wait instance id
   */
  public void setWaitInstanceId(String waitInstanceId) {
    this.waitInstanceId = waitInstanceId;
  }

  /**
   * Gets correlation id.
   *
   * @return the correlation id
   */
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * Sets correlation id.
   *
   * @param correlationId the correlation id
   */
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("waitInstanceId", waitInstanceId)
        .add("correlationId", correlationId)
        .toString();
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), waitInstanceId, correlationId);
  }
}
