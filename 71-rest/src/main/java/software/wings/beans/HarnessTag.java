package software.wings.beans;

import static software.wings.beans.HarnessTagType.USER;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.yaml.BaseYaml;

import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.jersey.JsonViews;
import software.wings.yaml.BaseEntityYaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@NgUniqueIndex(name = "tagIdx", fields = { @Field(HarnessTagKeys.accountId)
                                           , @Field(HarnessTagKeys.key) })
@Data
@Builder
@JsonInclude(Include.NON_NULL)
@FieldNameConstants(innerTypeName = "HarnessTagKeys")
@Entity(value = "tags", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class HarnessTag implements PersistentEntity, UuidAware, UpdatedAtAware, UpdatedByAware, CreatedAtAware,
                                   CreatedByAware, AccountAccess {
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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class HarnessTagAbstractYaml extends BaseYaml {
    private String name;
    private List<String> allowedValues;

    @lombok.Builder
    public HarnessTagAbstractYaml(String name, List<String> allowedValues) {
      this.name = name;
      this.allowedValues = allowedValues;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends BaseEntityYaml {
    private List<HarnessTagAbstractYaml> tag;

    @Builder
    public Yaml(String harnessApiVersion, List<HarnessTagAbstractYaml> tag) {
      super(EntityType.TAG.name(), harnessApiVersion);
      this.tag = tag;
    }
  }
}
