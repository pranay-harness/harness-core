package software.wings.beans;

import static java.lang.System.currentTimeMillis;
import static software.wings.common.UUIDGenerator.generateUuid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.UserThreadLocal;
import software.wings.utils.validation.Update;

import java.util.HashMap;
import java.util.Map;
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
@AllArgsConstructor
@NoArgsConstructor
public class Base implements UuidAware {
  public static final String APP_ID_KEY = "appId";
  public static final String ACCOUNT_ID_KEY = "accountId";

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

  @Getter(AccessLevel.NONE)
  @SchemaIgnore
  @JsonIgnore
  @Transient
  public transient String entityYamlPath; // TODO:: remove it with changeSet batching

  /**
   * Invoked before inserting document in mongo by morphia.
   */
  @PrePersist
  public void onSave() {
    if (uuid == null) {
      uuid = generateUuid();
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
      embeddedUser = EmbeddedUser.builder()
                         .uuid(UserThreadLocal.get().getUuid())
                         .email(UserThreadLocal.get().getEmail())
                         .name(UserThreadLocal.get().getName())
                         .build();
    }

    if (createdBy == null && !(this instanceof Account)) {
      createdBy = embeddedUser;
    }

    lastUpdatedAt = currentTimeMillis();
    lastUpdatedBy = embeddedUser;
  }

  @SchemaIgnore
  @JsonIgnore
  public Map<String, Object> getShardKeys() {
    Map shardKeys = new HashMap();
    shardKeys.put("appId", appId);
    return shardKeys;
  }
}
