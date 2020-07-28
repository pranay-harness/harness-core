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
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@ToString(exclude = {"credentials"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "GcpKmsConfigKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpKmsConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Project Id", required = true) private String projectId;

  @Attributes(title = "GCP Region", required = true) private String region;

  @Attributes(title = "Key Ring Name", required = true) private String keyRing;

  @Attributes(title = "Key Name", required = true) private String keyName;

  @Attributes(title = "GCP Service Account Credentials", required = true)
  @Encrypted(fieldName = "gcp_service_account_credentials")
  private char[] credentials;

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return "https://cloudkms.googleapis.com/";
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return EncryptionType.GCP_KMS + "-" + getName() + "-" + getUuid();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getEncryptionServiceUrl()));
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.GCP_KMS;
  }

  @Override
  public void maskSecrets() {
    this.credentials = SECRET_MASK.toCharArray();
  }

  @Override
  public boolean isGlobalKms() {
    return Account.GLOBAL_ACCOUNT_ID.equals(getAccountId());
  }

  @Override
  public SecretManagerConfigDTO toDTO() {
    throw new UnsupportedOperationException();
  }
}
