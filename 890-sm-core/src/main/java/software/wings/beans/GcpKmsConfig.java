package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SecretManagerCapabilities.CAN_BE_DEFAULT_SM;
import static io.harness.beans.SecretManagerCapabilities.CREATE_FILE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_INLINE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_FROM_SM;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_TO_SM;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.helpers.GlobalSecretManagerUtils.GLOBAL_ACCOUNT_ID;
import static io.harness.helpers.GlobalSecretManagerUtils.isNgHarnessSecretManager;
import static io.harness.security.encryption.SecretManagerType.KMS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@ToString(exclude = {"credentials"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "GcpKmsConfigKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpKmsConfig extends SecretManagerConfig {
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
  public SecretManagerType getType() {
    return KMS;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        getEncryptionServiceUrl(), maskingEvaluator));
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
    return GLOBAL_ACCOUNT_ID.equals(getAccountId()) || isNgHarnessSecretManager(getNgMetadata());
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
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    GcpKmsConfigDTO ngGcpKmsConfigDTO = GcpKmsConfigDTO.builder()
                                            .name(getName())
                                            .isDefault(isDefault())
                                            .encryptionType(getEncryptionType())
                                            .projectId(getProjectId())
                                            .keyRing(getKeyRing())
                                            .keyName(getKeyName())
                                            .region(getRegion())
                                            .build();
    SecretManagerConfigMapper.updateNGSecretManagerMetadata(getNgMetadata(), ngGcpKmsConfigDTO);
    if (!maskSecrets) {
      ngGcpKmsConfigDTO.setCredentials(getCredentials());
    }
    return ngGcpKmsConfigDTO;
  }
}
