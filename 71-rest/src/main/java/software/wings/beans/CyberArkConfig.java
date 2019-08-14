package software.wings.beans;

import static io.harness.expression.SecretString.SECRET_MASK;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.encryption.Encrypted;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

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
public class CyberArkConfig extends SecretManagerConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "CyberArk Url", required = true) private String cyberArkUrl;

  @Attributes(title = "App ID") @Encrypted private String appId;

  @Attributes(title = "Client Certificate") @Encrypted private String clientCertificate;

  @Override
  public void maskSecrets() {
    this.clientCertificate = SECRET_MASK;
  }

  @Override
  public String getEncryptionServiceUrl() {
    return cyberArkUrl;
  }

  @Override
  public String getValidationCriteria() {
    return EncryptionType.CYBERARK + "-" + getName() + "-" + getUuid();
  }

  public EncryptionType getEncryptionType() {
    return EncryptionType.CYBERARK;
  }
}
