package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
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

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsingh on 9/29/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"secretKey", "kmsArn"})
@EqualsAndHashCode(callSuper = false)
@Indexes({
  @Index(fields = { @Field("name"), @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Entity(value = "kmsConfig", noClassnameStored = true)
@HarnessExportableEntity
public class KmsConfig extends Base implements EncryptionConfig, ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true) @Encrypted private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true) @Encrypted private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted private String kmsArn;

  @Attributes(title = "AWS Region", required = true) private String region;

  private boolean isDefault = true;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;

  @JsonIgnore
  @SchemaIgnore
  public String getValidationCriteria() {
    return EncryptionType.KMS + "-" + getName() + "-" + getUuid();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(region));
  }
}
