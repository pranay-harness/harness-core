package software.wings.beans;

import static io.harness.beans.SecretManagerCapabilities.CAN_BE_DEFAULT_SM;
import static io.harness.beans.SecretManagerCapabilities.CREATE_FILE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_INLINE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_FROM_SM;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_TO_SM;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.helpers.GlobalSecretManagerUtils.GLOBAL_ACCOUNT_ID;
import static io.harness.security.encryption.SecretManagerType.KMS;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.encryption.Encrypted;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"accessKey", "secretKey", "kmsArn"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "KmsConfigKeys")
public class KmsConfig extends SecretManagerConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true)
  @Encrypted(fieldName = "aws_access_key")
  private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true)
  @Encrypted(fieldName = "aws_secret_key")
  private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted(fieldName = "aws_key_arn") private String kmsArn;

  @Attributes(title = "AWS Region", required = true) private String region;

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return KmsUtils.generateKmsUrl(region);
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return EncryptionType.KMS + "-" + getName() + "-" + getUuid();
  }

  @Override
  public SecretManagerType getType() {
    return KMS;
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
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    List<SecretManagerCapabilities> secretManagerCapabilities =
        Lists.newArrayList(CREATE_INLINE_SECRET, CREATE_FILE_SECRET, CAN_BE_DEFAULT_SM);
    if (!isTemplatized()) {
      secretManagerCapabilities.add(TRANSITION_SECRET_FROM_SM);
      secretManagerCapabilities.add(TRANSITION_SECRET_TO_SM);
    }
    return secretManagerCapabilities;
  }

  @Override
  public boolean isGlobalKms() {
    return GLOBAL_ACCOUNT_ID.equals(getAccountId());
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    throw new UnsupportedOperationException();
  }
}
