package software.wings.beans;

import static java.lang.System.currentTimeMillis;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;
import software.wings.common.UUIDGenerator;
import software.wings.security.UserThreadLocal;
import software.wings.utils.validation.Update;

import java.util.Comparator;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * The Base class is used to extend all the bean classes that requires persistence. The base class
 * includes common fields such as uuid, createdBy, create timestamp, updatedBy and update timestamp.
 * These fields are common for the beans that are persisted as documents in the mongo DB.
 *
 * @author Rishi
 */
public class Base {
  /**
   * The constant GLOBAL_APP_ID.
   */
  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  /**
   * The constant GLOBAL_ENV_ID.
   */
  public static final String GLOBAL_ENV_ID = "__GLOBAL_ENV_ID__";
  /**
   * The constant createdAtComparator.
   */
  public static final Comparator<Base> createdAtComparator = new Comparator<Base>() {

    @Override
    public int compare(Base o1, Base o2) {
      return new Long(o1.createdAt).compareTo(new Long(o2.createdAt));
    }
  };
  @Id @NotNull(groups = {Update.class}) private String uuid;
  @Indexed @NotNull private String appId;
  private EmbeddedUser createdBy;
  @Indexed private long createdAt;
  private EmbeddedUser lastUpdatedBy;
  private long lastUpdatedAt;
  @Indexed private boolean active = true;

  /**
   * Gets uuid.
   *
   * @return the uuid
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets created by.
   *
   * @return the created by
   */
  public EmbeddedUser getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets created by.
   *
   * @param createdBy the created by
   */
  public void setCreatedBy(EmbeddedUser createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Gets created at.
   *
   * @return the created at
   */
  public long getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets created at.
   *
   * @param createdAt the created at
   */
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Gets last updated by.
   *
   * @return the last updated by
   */
  public EmbeddedUser getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  /**
   * Sets last updated by.
   *
   * @param lastUpdatedBy the last updated by
   */
  public void setLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  /**
   * Gets last updated at.
   *
   * @return the last updated at
   */
  public long getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  /**
   * Sets last updated at.
   *
   * @param lastUpdatedAt the last updated at
   */
  public void setLastUpdatedAt(long lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  /**
   * Is active boolean.
   *
   * @return the boolean
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Sets active.
   *
   * @param active the active
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(uuid, appId, active);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Base other = (Base) obj;
    return Objects.equals(this.uuid, other.uuid) && Objects.equals(this.appId, other.appId)
        && Objects.equals(this.active, other.active);
  }

  /**
   * Invoked before inserting document in mongo by morphia.
   */
  @PrePersist
  public void onSave() {
    if (uuid == null) {
      uuid = UUIDGenerator.getUuid();
      if (this instanceof Application) {
        this.appId = uuid;
      }
    }
    if (createdAt == 0) {
      createdAt = currentTimeMillis();
    }

    User user = UserThreadLocal.get();

    EmbeddedUser embeddedUser = null;

    if (user != null) {
      embeddedUser = anEmbeddedUser()
                         .withUuid(UserThreadLocal.get().getUuid())
                         .withEmail(UserThreadLocal.get().getEmail())
                         .withName(UserThreadLocal.get().getName())
                         .build();
    }

    if (createdBy == null) {
      createdBy = embeddedUser;
    }

    lastUpdatedAt = currentTimeMillis();
    lastUpdatedBy = embeddedUser;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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
