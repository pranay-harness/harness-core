package software.wings.beans;

import static software.wings.beans.HarnessTagType.USER;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@JsonInclude(Include.NON_NULL)
@FieldNameConstants(innerTypeName = "HarnessTagKeys")
@Entity(value = "tags", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class HarnessTag implements PersistentEntity, UuidAware, UpdatedAtAware, UpdatedByAware, CreatedAtAware,
                                   CreatedByAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .field(HarnessTagKeys.accountId)
                 .field(HarnessTagKeys.key)
                 .unique(true)
                 .name("tagIdx")
                 .build())
        .build();
  }

  @Id private String uuid;
  @NotEmpty private String accountId;
  private String key;
  @Builder.Default private HarnessTagType tagType = USER;
  private Set<String> allowedValues;
  private transient Set<String> inUseValues;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private EmbeddedUser createdBy;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;
}
