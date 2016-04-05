package software.wings.beans;

import com.google.common.base.MoreObjects;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;

import org.mongodb.morphia.annotations.Reference;
import software.wings.common.UUIDGenerator;
import software.wings.utils.validation.Update;

import javax.validation.constraints.NotNull;

/**
 *  The Base class is used to extend all the bean classes that requires persistence. The base class includes
 *  common fields such as uuid, createdBy, create timestamp, updatedBy and update timestamp. These fields are
 *  common for the beans that are persisted as documents in the mongo DB.
 *
 *
 * @author Rishi
 *
 */
public class Base {
  @Id @NotNull(groups = {Update.class}) private String uuid;

  @Reference(idOnly = true, ignoreMissing = true) private User createdBy;

  @Indexed private long createdAt;

  @Reference(idOnly = true, ignoreMissing = true) private User lastUpdatedBy;

  private long lastUpdatedAt;

  @Indexed private boolean active = true;

  public String getUuid() {
    return uuid;
  }
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
  public User getCreatedBy() {
    return createdBy;
  }
  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }
  public long getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }
  public void setLastUpdatedBy(User lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }
  public long getLastUpdatedAt() {
    return lastUpdatedAt;
  }
  public void setLastUpdatedAt(long lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }
  public boolean isActive() {
    return active;
  }
  public void setActive(boolean active) {
    this.active = active;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Base other = (Base) obj;
    if (uuid == null) {
      if (other.uuid != null)
        return false;
    } else if (!uuid.equals(other.uuid))
      return false;
    return true;
  }
  @PrePersist
  public void onUpdate() {
    lastUpdatedAt = System.currentTimeMillis();
    if (uuid == null) {
      uuid = UUIDGenerator.getUUID();
    }
    if (createdAt == 0) {
      createdAt = System.currentTimeMillis();
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uuid", uuid)
        .add("createdBy", createdBy)
        .add("createdAt", createdAt)
        .add("lastUpdatedBy", lastUpdatedBy)
        .add("lastUpdatedAt", lastUpdatedAt)
        .add("active", active)
        .toString();
  }
}
