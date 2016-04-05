package software.wings.waitNotify;

import java.io.Serializable;
import java.util.Date;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Serialized;

import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;

/**
 * @author Rishi
 *
 */
@Embedded
@Entity(value = "notifyResponses", noClassnameStored = true)
public class NotifyResponse extends Base {
  @Serialized private Serializable response;

  @Indexed private Date expiryTs;

  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  public NotifyResponse() {}

  public NotifyResponse(String correlationId, Serializable response) {
    setUuid(correlationId);
    setResponse(response);
  }
  public Serializable getResponse() {
    return response;
  }
  public void setResponse(Serializable response) {
    this.response = response;
  }

  public Date getExpiryTs() {
    return expiryTs;
  }

  public void setExpiryTs(Date expiryTs) {
    this.expiryTs = expiryTs;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }
}
