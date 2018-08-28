package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;

@Entity(value = "resourceConstraintInstances", noClassnameStored = true)
@EqualsAndHashCode(callSuper = false)
@Data
@Indexes(@Index(options = @IndexOptions(unique = true, name = "uniqueOrder"),
    fields = { @Field("resourceConstraintId")
               , @Field("order") }))
public class ResourceConstraintInstance extends Base {
  public static final String RELEASE_ENTITY_TYPE_KEY = "releaseEntityType";
  public static final String RELEASE_ENTITY_ID_KEY = "releaseEntityId";
  public static final String RESOURCE_CONSTRAINT_ID_KEY = "resourceConstraintId";
  public static final String STATE_KEY = "state";
  public static final String ORDER_KEY = "order";

  private String accountId;

  @Indexed private String resourceConstraintId;
  private int order;

  @Indexed private String state;
  private int permits;

  private String releaseEntityType;
  private String releaseEntityId;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @Builder
  private ResourceConstraintInstance(String uuid, String accountId, String appId, String resourceConstraintId,
      int order, String state, int permits, String releaseEntityType, String releaseEntityId) {
    setUuid(uuid);
    setAccountId(accountId);
    setAppId(appId);
    this.resourceConstraintId = resourceConstraintId;
    this.order = order;
    this.state = state;
    this.permits = permits;
    this.releaseEntityType = releaseEntityType;
    this.releaseEntityId = releaseEntityId;
  }
}
