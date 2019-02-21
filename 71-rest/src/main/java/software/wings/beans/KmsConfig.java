package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import io.harness.encryption.Encrypted;
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
 * Created by rsingh on 9/29/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Indexes({
  @Index(fields = { @Field("name"), @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Entity(value = "kmsConfig", noClassnameStored = true)
@HarnessExportableEntity
@ToString(exclude = {"secretKey", "kmsArn"})
public class KmsConfig extends Base implements EncryptionConfig {
  @Attributes(title = "Name", required = true) @NaturalKey private String name;

  @Attributes(title = "AWS Access Key", required = true) @Encrypted private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true) @Encrypted private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted private String kmsArn;

  @Attributes(title = "AWS Region", required = true) private String region;

  private boolean isDefault = true;

  @SchemaIgnore @NotEmpty @NaturalKey private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;

  @JsonIgnore
  @SchemaIgnore
  public String getValidationCriteria() {
    return EncryptionType.KMS + "-" + getName() + "-" + getUuid();
  }
}
