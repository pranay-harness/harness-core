package software.wings.delegatetasks.validation.capabilities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.helpers.ext.mail.SmtpConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._930_DELEGATE_TASKS)
public class SmtpCapability implements ExecutionCapability {
  @NotNull private SmtpConfig smtpConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SMTP;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return smtpConfig.getHost() + ":" + smtpConfig.getPort();
  }
}
