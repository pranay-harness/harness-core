package software.wings.beans;

import static io.harness.beans.SecretManagerCapabilities.CREATE_REFERENCE_SECRET;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
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
@ToString(exclude = {"clientCertificate"})
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "CyberArkConfigKeys")
public class CyberArkConfig extends SecretManagerConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "CyberArk Url", required = true) private String cyberArkUrl;

  @Attributes(title = "App ID") private String appId;

  @JsonIgnore @SchemaIgnore boolean isCertValidationRequired;

  @Attributes(title = "Client Certificate")
  @Encrypted(fieldName = "client_certificate")
  private String clientCertificate;

  @Override
  public void maskSecrets() {
    this.clientCertificate = SECRET_MASK;
  }

  @Override
  public String getEncryptionServiceUrl() {
    return cyberArkUrl;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getEncryptionServiceUrl()));
  }

  @Override
  public String getValidationCriteria() {
    return EncryptionType.CYBERARK + "-" + getName() + "-" + getUuid();
  }

  @Override
  public SecretManagerType getType() {
    return VAULT;
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.CYBERARK;
  }

  @Override
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    return Lists.newArrayList(CREATE_REFERENCE_SECRET);
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    throw new UnsupportedOperationException();
  }

  public boolean isCertValidationRequired() {
    return isCertValidationRequired;
  }
}
