package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import software.wings.beans.entityinterface.ApplicationAccess;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "SSOSettingsKeys")
@Entity(value = "ssoSettings")
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class SSOSettings
    implements AccountAccess, PersistentRegularIterable, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
               UpdatedAtAware, UpdatedByAware, ApplicationAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountIdTypeIdx")
                 .field(SSOSettingsKeys.accountId)
                 .field(SSOSettingsKeys.type)
                 .build())
        .build();
  }

  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";
  @Deprecated public static final String ACCOUNT_ID_KEY2 = "accountId";

  @Id private String uuid;
  @FdIndex @NotNull @SchemaIgnore protected String appId;

  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @FdIndex private long createdAt;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotNull protected SSOType type;
  @NotEmpty protected String displayName;
  @NotEmpty protected String url;
  private Long nextIteration;

  public SSOSettings(SSOType type, String displayName, String url) {
    this.type = type;
    this.displayName = displayName;
    this.url = url;
    appId = GLOBAL_APP_ID;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return this.nextIteration;
  }

  // TODO: Return list of all sso settings instead with the use of @JsonIgnore to trim the unnecessary elements
  @JsonIgnore public abstract SSOSettings getPublicSSOSettings();

  public abstract SSOType getType();

  public static final class SSOSettingsKeys { public static final String accountId = AccountAccess.ACCOUNT_ID_KEY; }
}
