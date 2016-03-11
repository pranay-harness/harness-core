package software.wings.lock;

import java.util.Date;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import software.wings.beans.Base;

/**
 *  Lock bean class
 *
 *
 * @author Rishi
 *
 */
@Embedded
@Entity(value = "locks", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("entityType")
                           , @Field("entityId") }, options = @IndexOptions(unique = true)))
public class Lock extends Base {
  private String entityType;
  private String entityId;

  private String hostName;
  private String ipAddress;

  private String threadName;
  private long threadId;

  @Indexed private Date expiryDate;

  public String getEntityType() {
    return entityType;
  }
  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }
  public String getEntityId() {
    return entityId;
  }
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }
  public String getHostName() {
    return hostName;
  }
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }
  public String getIpAddress() {
    return ipAddress;
  }
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }
  public Date getExpiryDate() {
    return expiryDate;
  }
  public void setExpiryDate(Date expiryDate) {
    this.expiryDate = expiryDate;
  }
  public String getThreadName() {
    return threadName;
  }
  public void setThreadName(String threadName) {
    this.threadName = threadName;
  }
  public long getThreadId() {
    return threadId;
  }
  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }
}
