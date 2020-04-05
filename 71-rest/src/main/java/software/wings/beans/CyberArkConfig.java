package software.wings.beans;

import static io.harness.expression.SecretString.SECRET_MASK;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
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

import java.util.Arrays;
import java.util.List;

/**
 * @author marklu on 2019-08-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"clientCertificate"})
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "CyberArkConfigKeys")
public class CyberArkConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "CyberArk Url", required = true) private String cyberArkUrl;

  @Attributes(title = "App ID") private String appId;

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
  public EncryptionType getEncryptionType() {
    return EncryptionType.CYBERARK;
  }
}
