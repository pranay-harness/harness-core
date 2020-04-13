package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.validation.Update;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;

import javax.validation.constraints.NotNull;

/**
 * This is a shared persistent entity to track Secret Managers of different type (KMS/AWS Secrets Manager/Vault etc.) in
 * a centralized location to simplify the logic to track which secret manager is the account level default.
 *
 *
 * @author marklu on 2019-05-31
 */
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"createdBy", "createdAt", "lastUpdatedBy", "lastUpdatedAt"})
@Entity(value = "secretManagers")
@Indexes({
  @Index(fields = {
    @Field("name"), @Field("accountId"), @Field("encryptionType")
  }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "SecretManagerConfigKeys")
public abstract class SecretManagerConfig
    implements EncryptionConfig, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
               UpdatedByAware, PersistentRegularIterable, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  private EncryptionType encryptionType;

  private boolean isDefault;

  @NotEmpty @Indexed private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private String encryptedBy;

  @SchemaIgnore private EmbeddedUser createdBy;

  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;

  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @Indexed private Long nextTokenRenewIteration;

  public abstract void maskSecrets();

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (SecretManagerConfigKeys.nextTokenRenewIteration.equals(fieldName)) {
      this.nextTokenRenewIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (SecretManagerConfigKeys.nextTokenRenewIteration.equals(fieldName)) {
      return nextTokenRenewIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  @JsonIgnore
  public boolean isGlobalKms() {
    return false;
  }
}
