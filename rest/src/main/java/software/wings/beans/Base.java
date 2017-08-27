package software.wings.beans;

import static java.lang.System.currentTimeMillis;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;
import software.wings.common.UUIDGenerator;
import software.wings.security.UserThreadLocal;
import software.wings.utils.validation.Update;

import javax.validation.constraints.NotNull;

/**
 * The Base class is used to extend all the bean classes that requires persistence. The base class
 * includes common fields such as uuid, createdBy, create timestamp, updatedBy and update timestamp.
 * These fields are common for the beans that are persisted as documents in the mongo DB.
 *
 * @author Rishi
 */
@Data
@EqualsAndHashCode(of = {"uuid", "appId"})
public class Base implements UuidAware {
  /**
   * The constant GLOBAL_APP_ID.
   */
  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  /**
   * The constant GLOBAL_ENV_ID.
   */
  public static final String GLOBAL_ENV_ID = "__GLOBAL_ENV_ID__";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore private long lastUpdatedAt;

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

    if (createdBy == null && !(this instanceof Account)) {
      createdBy = embeddedUser;
    }

    lastUpdatedAt = currentTimeMillis();
    lastUpdatedBy = embeddedUser;
  }
}
