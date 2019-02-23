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
@Data
@EqualsAndHashCode(callSuper = false)
@Indexes(@Index(options = @IndexOptions(unique = true, name = "uniqueUnitOrder"),
    fields = { @Field("resourceConstraintId")
               , @Field("resourceUnit"), @Field("order") }))
public class ResourceConstraintInstance extends Base {
  public static final String ACQUIRED_AT_KEY = "acquiredAt";
  public static final String ORDER_KEY = "order";
  public static final String RELEASE_ENTITY_ID_KEY = "releaseEntityId";
  public static final String RELEASE_ENTITY_TYPE_KEY = "releaseEntityType";
  public static final String RESOURCE_CONSTRAINT_ID_KEY = "resourceConstraintId";
  public static final String RESOURCE_UNIT_KEY = "resourceUnit";
  public static final String STATE_KEY = "state";

  private String accountId;

  private String resourceConstraintId;
  private String resourceUnit;
  private int order;

  @Indexed private String state;
  private int permits;

  private String releaseEntityType;
  private String releaseEntityId;

  private long acquiredAt;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @Builder
  private ResourceConstraintInstance(String uuid, String accountId, String appId, String resourceConstraintId,
      String resourceUnit, int order, String state, int permits, String releaseEntityType, String releaseEntityId,
      long acquiredAt) {
    setUuid(uuid);
    setAccountId(accountId);
    setAppId(appId);
    this.resourceConstraintId = resourceConstraintId;
    this.resourceUnit = resourceUnit;
    this.order = order;
    this.state = state;
    this.permits = permits;
    this.releaseEntityType = releaseEntityType;
    this.releaseEntityId = releaseEntityId;
    this.acquiredAt = acquiredAt;
  }
}
