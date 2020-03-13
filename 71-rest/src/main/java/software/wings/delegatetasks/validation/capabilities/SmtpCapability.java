package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class SmtpCapability implements ExecutionCapability {
  @NotNull private SmtpConfig smtpConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SMTP;

  @Override
  public String fetchCapabilityBasis() {
    return smtpConfig.getHost() + ":" + smtpConfig.getPort();
  }
}
