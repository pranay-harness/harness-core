package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by rsingh on 11/02/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Indexes({
  @Index(fields = { @Field("name")
                    , @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
  , @Index(fields = {
    @Field("vaultUrl"), @Field("accountId")
  }, options = @IndexOptions(unique = true, name = "uniqueUrlIdx"))
})
@HarnessExportableEntity
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "vaultConfig", noClassnameStored = true)
@ToString(exclude = {"authToken"})
public class VaultConfig extends Base implements EncryptionConfig {
  @Attributes(title = "Name", required = true) @NaturalKey private String name;

  @Attributes(title = "Vault Url", required = true) @NaturalKey private String vaultUrl;

  @Attributes(title = "Auth token", required = true) private String authToken;

  @Attributes(title = "Base Path") private String basePath;

  @Attributes(title = "Renew token interval", required = true) private int renewIntervalHours;

  /**
   * Vault 0.11 is using secrete engine V2 by default and it mandate a slightly different way of read/write secrets
   * This field should have value "1" or "2". For backward compatibility, null of value "0" will be converted to value
   * "1" automatically.
   */
  @SchemaIgnore private int secretEngineVersion;

  @Builder.Default private boolean isDefault = true;

  private long renewedAt;

  @SchemaIgnore @NotEmpty @NaturalKey private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;

  @SchemaIgnore @Transient private String encryptedBy;

  @JsonIgnore
  @SchemaIgnore
  public String getValidationCriteria() {
    return getVaultUrl();
  }
}
