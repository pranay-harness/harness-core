package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.persistence.AccountAccess;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsingh on 11/02/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"authToken"})
@EqualsAndHashCode(callSuper = false)
@Indexes({
  @Index(fields = { @Field("name")
                    , @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
  , @Index(fields = {
    @Field("vaultUrl"), @Field("accountId")
  }, options = @IndexOptions(unique = true, name = "uniqueUrlIdx"))
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "vaultConfig", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "VaultConfigKeys")
public class VaultLegacyConfig extends Base implements EncryptionConfig, ExecutionCapabilityDemander, AccountAccess {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Vault Url", required = true) private String vaultUrl;

  @Attributes(title = "Auth token") private String authToken;

  @Attributes(title = "AppRole Id") private String appRoleId;

  @Attributes(title = "Secret Id") private String secretId;

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

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;

  @SchemaIgnore @Transient private String encryptedBy;

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return vaultUrl;
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return vaultUrl;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(vaultUrl));
  }

  @Override
  public boolean isGlobalKms() {
    return false;
  }
}
