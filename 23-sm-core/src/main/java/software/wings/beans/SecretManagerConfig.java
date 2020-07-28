package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.ng.core.NGAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTOConverter;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.validation.Update;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.Optional;
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
@CdUniqueIndex(name = "uniqueIdx", fields = { @Field("name")
                                              , @Field("accountId"), @Field("encryptionType") })
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "SecretManagerConfigKeys")
public abstract class SecretManagerConfig
    implements EncryptionConfig, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
               UpdatedByAware, PersistentRegularIterable, NGAccess, NGSecretManagerConfigDTOConverter {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  private EncryptionType encryptionType;

  private boolean isDefault;

  @NotEmpty @FdIndex private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private String encryptedBy;

  @SchemaIgnore private EmbeddedUser createdBy;

  @SchemaIgnore @FdIndex private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;

  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @FdIndex private Long nextTokenRenewIteration;

  @JsonIgnore private NGSecretManagerMetadata ngMetadata;

  private List<String> templatizedFields;

  public static boolean isTemplatized(SecretManagerConfig secretManagerConfig) {
    return !isEmpty(secretManagerConfig.getTemplatizedFields());
  }

  public abstract void maskSecrets();

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
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

  @Override
  @JsonIgnore
  public String getAccountIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGSecretManagerMetadata::getAccountIdentifier).orElse(null);
  }

  @Override
  @JsonIgnore
  public String getOrgIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGSecretManagerMetadata::getOrgIdentifier).orElse(null);
  }

  @Override
  @JsonIgnore
  public String getProjectIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGSecretManagerMetadata::getProjectIdentifier).orElse(null);
  }

  @Override
  @JsonIgnore
  public String getIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGSecretManagerMetadata::getIdentifier).orElse(null);
  }
}
