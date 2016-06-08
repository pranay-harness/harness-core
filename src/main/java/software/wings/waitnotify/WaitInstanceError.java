package software.wings.waitnotify;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Serialized;
import software.wings.beans.Base;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Represents errors thrown by callback of wait instance.
 *
 * @author Rishi
 */
@Entity(value = "waitInstanceErrors", noClassnameStored = true)
public class WaitInstanceError extends Base {
  private String waitInstanceId;

  @Serialized private Map<String, Serializable> responseMap;

  private String errorStackTrace;

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
   * Gets response map.
   *
   * @return the response map
   */
  public Map<String, Serializable> getResponseMap() {
    return responseMap;
  }

  /**
   * Sets response map.
   *
   * @param responseMap the response map
   */
  public void setResponseMap(Map<String, Serializable> responseMap) {
    this.responseMap = responseMap;
  }

  /**
   * Gets error stack trace.
   *
   * @return the error stack trace
   */
  public String getErrorStackTrace() {
    return errorStackTrace;
  }

  /**
   * Sets error stack trace.
   *
   * @param errorStackTrace the error stack trace
   */
  public void setErrorStackTrace(String errorStackTrace) {
    this.errorStackTrace = errorStackTrace;
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
    WaitInstanceError that = (WaitInstanceError) obj;
    return Objects.equals(waitInstanceId, that.waitInstanceId) && Objects.equals(responseMap, that.responseMap)
        && Objects.equals(errorStackTrace, that.errorStackTrace);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), waitInstanceId, responseMap, errorStackTrace);
  }
}
