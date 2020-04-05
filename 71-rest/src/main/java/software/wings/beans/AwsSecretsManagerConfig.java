package software.wings.beans;

import static io.harness.expression.SecretString.SECRET_MASK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
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
 * @author marklu on 2019-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"secretKey"})
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AwsSecretsManagerConfigKeys")
public class AwsSecretsManagerConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true) private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true)
  @Encrypted(fieldName = "aws_secret_key")
  private String secretKey;

  @Attributes(title = "AWS Region", required = true) private String region;

  @Attributes(title = "Secret Name Prefix") private String secretNamePrefix;

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
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getEncryptionServiceUrl()));
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.AWS_SECRETS_MANAGER;
  }

  @Override
  public void maskSecrets() {
    this.secretKey = SECRET_MASK;
  }
}
