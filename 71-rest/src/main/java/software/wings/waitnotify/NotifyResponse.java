package software.wings.waitnotify;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.task.protocol.ResponseData;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * Represents response generated by a correlationId.
 *
 * @param <T> the generic type
 * @author Rishi
 */
@Entity(value = "notifyResponses", noClassnameStored = true)
public class NotifyResponse<T extends ResponseData> extends Base {
  public static final String STATUS_KEY = "status";

  private T response;

  private boolean error;

  @Indexed private Date expiryTs;

  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  /**
   * Instantiates a new notify response.
   */
  public NotifyResponse() {}

  /**
   * Instantiates a new notify response.
   *
   * @param correlationId the correlation id
   * @param response      the response
   */
  public NotifyResponse(String correlationId, T response) {
    this(correlationId, response, false);
  }

  public NotifyResponse(String correlationId, T response, boolean error) {
    setUuid(correlationId);
    setResponse(response);
    setError(error);
  }

  /**
   * Gets response.
   *
   * @return the response
   */
  public T getResponse() {
    return response;
  }

  /**
   * Sets response.
   *
   * @param response the response
   */
  public void setResponse(T response) {
    this.response = response;
  }

  /**
   * Gets expiry ts.
   *
   * @return the expiry ts
   */
  public Date getExpiryTs() {
    return expiryTs == null ? null : new Date(expiryTs.getTime());
  }

  /**
   * Sets expiry ts.
   *
   * @param expiryTs the expiry ts
   */
  public void setExpiryTs(Date expiryTs) {
    this.expiryTs = new Date(expiryTs.getTime());
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Getter for property 'error'.
   *
   * @return Value for property 'error'.
   */
  public boolean isError() {
    return error;
  }

  /**
   * Setter for property 'error'.
   *
   * @param error Value to set for property 'error'.
   */
  public void setError(boolean error) {
    this.error = error;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(response, error, expiryTs, status);
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
    final NotifyResponse other = (NotifyResponse) obj;
    return Objects.equals(this.response, other.response) && Objects.equals(this.error, other.error)
        && Objects.equals(this.expiryTs, other.expiryTs) && Objects.equals(this.status, other.status);
  }
}
