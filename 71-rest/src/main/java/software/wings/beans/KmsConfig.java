package software.wings.beans;

import static io.harness.expression.SecretString.SECRET_MASK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import software.wings.delegatetasks.validation.AbstractSecretManagerValidation;

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
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "KmsConfigKeys")
public class KmsConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true) private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true) private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) private String kmsArn;

  @Attributes(title = "AWS Region", required = true) private String region;

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
    return EncryptionType.KMS + "-" + getName() + "-" + getUuid();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(region));
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.KMS;
  }

  @Override
  public void maskSecrets() {
    this.secretKey = SECRET_MASK;
    this.kmsArn = SECRET_MASK;
  }

  @Override
  public boolean isGlobalKms() {
    return Account.GLOBAL_ACCOUNT_ID.equals(getAccountId());
  }
}
