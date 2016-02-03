package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;

/**
 *  HttpAuditPayload bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "auditPayloads", noClassnameStored = true)
public class AuditPayload extends Base {
  public enum RequestType { REQUEST, RESPONSE }
  ;

  @Indexed private RequestType requestType;

  @Indexed private String headerId;

  private byte[] payload;

  public RequestType getRequestType() {
    return requestType;
  }
  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public String getHeaderId() {
    return headerId;
  }
  public void setHeaderId(String headerId) {
    this.headerId = headerId;
  }
  public byte[] getPayload() {
    return payload;
  }
  public void setPayload(byte[] payload) {
    this.payload = payload;
  }
}
