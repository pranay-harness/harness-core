package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.encryption.Encrypted;
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
import software.wings.delegatetasks.validation.AbstractSecretManagerValidation;

/**
 * @author marklu on 2019-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"secretKey"})
@EqualsAndHashCode(callSuper = false)
@Indexes({
  @Index(fields = { @Field("name"), @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Entity(value = "awsSecretsManagerConfig", noClassnameStored = true)
@HarnessEntity(exportable = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AwsSecretsManagerConfigKeys")
public class AwsSecretsManagerLegacyConfig extends Base implements EncryptionConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true)
  @Encrypted(fieldName = "aws_access_key")
  private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true)
  @Encrypted(fieldName = "aws_secret_key")
  private String secretKey;

  @Attributes(title = "AWS Region", required = true) private String region;

  @Attributes(title = "Secret Name Prefix") private String secretNamePrefix;

  private boolean isDefault = true;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;

  @SchemaIgnore @Transient private String encryptedBy;

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return AbstractSecretManagerValidation.getAwsUrlFromRegion(region);
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return EncryptionType.AWS_SECRETS_MANAGER + "-" + getName() + "-" + getUuid();
  }

  @Override
  public boolean isGlobalKms() {
    return false;
  }
}
