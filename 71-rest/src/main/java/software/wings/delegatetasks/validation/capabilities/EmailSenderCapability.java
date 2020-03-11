package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Value;
import software.wings.helpers.ext.external.comm.EmailRequest;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class EmailSenderCapability implements ExecutionCapability {
  @NotNull private EmailRequest emailRequest;
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.EMAIL_SENDER;

  @Override
  public String fetchCapabilityBasis() {
    return emailRequest.getSmtpConfig().getHost() + ":" + emailRequest.getSmtpConfig().getPort();
  }
}
