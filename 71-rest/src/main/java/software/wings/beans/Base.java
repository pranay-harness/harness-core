package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.UserThreadLocal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
@EqualsAndHashCode(of = {"uuid", "appId"}, callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class Base extends PersistentEntity implements UuidAware {
  public static final String ID_KEY = "_id";
  public static final String APP_ID_KEY = "appId";
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String LAST_UPDATED_AT_KEY = "lastUpdatedAt";
  public static final String CREATED_AT_KEY = "createdAt";
  public static final String CREATED_BY_KEY = "createdBy";

  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  public static final String KEYWORDS_KEY = "keywords";

  public static final String LINKED_TEMPLATE_UUIDS_KEY = "linkedTemplateUuids";

  public static final String TEMPATE_UUID_KEY = "templateUuid";

  /**
   * The constant GLOBAL_ENV_ID.
   */
  public static final String GLOBAL_ENV_ID = "__GLOBAL_ENV_ID__";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @SchemaIgnore @Indexed private List<String> keywords;

  /**
   * TODO: Add isDeleted boolean field to enable soft delete. @swagat
   */

  @SchemaIgnore
  @JsonIgnore
  @Transient
  private transient String entityYamlPath; // TODO:: remove it with changeSet batching

  @JsonIgnore
  @SchemaIgnore
  public String getEntityYamlPath() {
    return entityYamlPath;
  }

  @JsonIgnore @SchemaIgnore @Setter private transient boolean syncFromGit;

  @JsonIgnore
  @SchemaIgnore
  public boolean isSyncFromGit() {
    return syncFromGit;
  }

  public Base(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath) {
    this.uuid = uuid;
    this.appId = appId;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.lastUpdatedBy = lastUpdatedBy;
    this.lastUpdatedAt = lastUpdatedAt;
    this.keywords = keywords;
    this.entityYamlPath = entityYamlPath;
  }

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
    EmbeddedUser embeddedUser = prepareEmbeddedUser();
    if (createdBy == null && !(this instanceof Account)) {
      createdBy = embeddedUser;
    }

    lastUpdatedAt = currentTimeMillis();
    lastUpdatedBy = embeddedUser;
  }

  @SchemaIgnore
  @JsonIgnore
  public Map<String, Object> getShardKeys() {
    Map<String, Object> shardKeys = new HashMap<>();
    shardKeys.put("appId", appId);
    return shardKeys;
  }

  public List<Object> generateKeywords() {
    EmbeddedUser embeddedUser = prepareEmbeddedUser();
    if (createdBy == null) {
      createdBy = embeddedUser;
    }
    List<Object> keyWordList = new ArrayList<>();
    if (createdBy != null) {
      keyWordList.add(createdBy.getName());
      keyWordList.add(createdBy.getEmail());
    }

    if (lastUpdatedBy == null) {
      lastUpdatedBy = embeddedUser;
    }
    if (lastUpdatedBy != null && !lastUpdatedBy.equals(createdBy)) {
      keyWordList.add(lastUpdatedBy.getName());
      keyWordList.add(lastUpdatedBy.getEmail());
    }
    return keyWordList;
  }

  private EmbeddedUser prepareEmbeddedUser() {
    User user = UserThreadLocal.get();
    if (user != null) {
      return EmbeddedUser.builder()
          .uuid(UserThreadLocal.get().getUuid())
          .email(UserThreadLocal.get().getEmail())
          .name(UserThreadLocal.get().getName())
          .build();
    }
    return null;
  }
}
