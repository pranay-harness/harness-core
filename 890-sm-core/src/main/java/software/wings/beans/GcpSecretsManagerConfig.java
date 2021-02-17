package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SecretManagerCapabilities.CAN_BE_DEFAULT_SM;
import static io.harness.beans.SecretManagerCapabilities.CREATE_FILE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_INLINE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_REFERENCE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_FROM_SM;
import static io.harness.beans.SecretManagerCapabilities.TRANSITION_SECRET_TO_SM;
import static io.harness.expression.SecretString.SECRET_MASK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Data
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@ToString(exclude = {"credentials"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "GcpSecretsManagerConfigKeys")
public class GcpSecretsManagerConfig extends SecretManagerConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "GCP Service Account Credentials", required = true)
  @Encrypted(fieldName = "gcp_service_account_credentials")
  private char[] credentials;

  @Override
  public void maskSecrets() {
    this.credentials = SECRET_MASK.toCharArray();
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return "https://secretmanager.googleapis.com/v1/";
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return EncryptionType.GCP_SECRETS_MANAGER + "-" + getName() + "-" + getUuid();
  }

  @Override
  public SecretManagerType getType() {
    return SecretManagerType.VAULT;
  }

  @Override
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    List<SecretManagerCapabilities> secretManagerCapabilities =
        Lists.newArrayList(CREATE_INLINE_SECRET, CREATE_REFERENCE_SECRET, CREATE_FILE_SECRET, CAN_BE_DEFAULT_SM);
    if (!isTemplatized()) {
      secretManagerCapabilities.add(TRANSITION_SECRET_FROM_SM);
      secretManagerCapabilities.add(TRANSITION_SECRET_TO_SM);
    }
    return secretManagerCapabilities;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        getEncryptionServiceUrl(), maskingEvaluator));
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    return null;
  }
}
