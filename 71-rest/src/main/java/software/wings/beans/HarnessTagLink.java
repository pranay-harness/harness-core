package software.wings.beans;

import static software.wings.beans.HarnessTagType.USER;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)

@NgUniqueIndex(name = "entityTagIdx",
    fields =
    { @Field(HarnessTagLinkKeys.accountId)
      , @Field(HarnessTagLinkKeys.entityId), @Field(HarnessTagLinkKeys.key) })
@CdIndex(name = "tagValueIdx",
    fields = { @Field(HarnessTagLinkKeys.accountId)
               , @Field(HarnessTagLinkKeys.key), @Field(HarnessTagLinkKeys.value) })
@Data
@Builder
@JsonInclude(Include.NON_NULL)
@FieldNameConstants(innerTypeName = "HarnessTagLinkKeys")
@Entity(value = "tagLinks", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class HarnessTagLink implements PersistentEntity, UuidAware, UpdatedAtAware, UpdatedByAware, CreatedAtAware,
                                       CreatedByAware, AccountAccess {
  @Id private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotEmpty private String key;
  private String value;
  @NotNull private EntityType entityType;
  @NotEmpty private String entityId;
  @Builder.Default private HarnessTagType tagType = USER;

  private transient String appName;
  private transient String entityName;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private EmbeddedUser createdBy;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;
}
