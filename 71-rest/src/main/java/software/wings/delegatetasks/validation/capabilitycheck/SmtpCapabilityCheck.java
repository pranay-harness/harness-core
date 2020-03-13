package software.wings.delegatetasks.validation.capabilitycheck;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.validation.capabilities.SmtpCapability;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;

@Slf4j
public class SmtpCapabilityCheck implements CapabilityCheck {
  @Inject private EmailHandler emailHandler;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SmtpCapability smtpCapability = (SmtpCapability) delegateCapability;
    return CapabilityResponse.builder()
        .delegateCapability(smtpCapability)
        .validated(emailHandler.validateDelegateConnection(
            smtpCapability.getSmtpConfig(), smtpCapability.getEncryptionDetails()))
        .build();
  }
}
