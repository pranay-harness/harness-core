package software.wings.lock;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.util.Date;

/**
 * Lock bean class.
 *
 * @author Rishi
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

  /**
   * Gets entity type.
   *
   * @return the entity type
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Sets entity type.
   *
   * @param entityType the entity type
   */
  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  /**
   * Gets entity id.
   *
   * @return the entity id
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Sets entity id.
   *
   * @param entityId the entity id
   */
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets ip address.
   *
   * @return the ip address
   */
  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Sets ip address.
   *
   * @param ipAddress the ip address
   */
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  /**
   * Gets expiry date.
   *
   * @return the expiry date
   */
  public Date getExpiryDate() {
    return expiryDate;
  }

  /**
   * Sets expiry date.
   *
   * @param expiryDate the expiry date
   */
  public void setExpiryDate(Date expiryDate) {
    this.expiryDate = expiryDate;
  }

  /**
   * Gets thread name.
   *
   * @return the thread name
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * Sets thread name.
   *
   * @param threadName the thread name
   */
  public void setThreadName(String threadName) {
    this.threadName = threadName;
  }

  /**
   * Gets thread id.
   *
   * @return the thread id
   */
  public long getThreadId() {
    return threadId;
  }

  /**
   * Sets thread id.
   *
   * @param threadId the thread id
   */
  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }
}
