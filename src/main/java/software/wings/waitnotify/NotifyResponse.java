package software.wings.waitnotify;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Serialized;
import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Represents response generated by a correlationId.
 * @author Rishi
 */
@Embedded
@Entity(value = "notifyResponses", noClassnameStored = true)
public class NotifyResponse<T extends Serializable> extends Base {
  @Serialized private T response;

  @Indexed private Date expiryTs;

  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  public NotifyResponse() {}

  public NotifyResponse(String correlationId, T response) {
    setUuid(correlationId);
    setResponse(response);
  }

  public T getResponse() {
    return response;
  }

  public void setResponse(T response) {
    this.response = response;
  }

  public Date getExpiryTs() {
    return new Date(expiryTs.getTime());
  }

  public void setExpiryTs(Date expiryTs) {
    this.expiryTs = new Date(expiryTs.getTime());
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
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
    NotifyResponse<?> that = (NotifyResponse<?>) obj;
    return Objects.equals(response, that.response) && Objects.equals(expiryTs, that.expiryTs) && status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), response, expiryTs, status);
  }
}
