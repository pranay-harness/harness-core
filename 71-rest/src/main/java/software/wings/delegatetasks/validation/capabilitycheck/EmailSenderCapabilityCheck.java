package software.wings.delegatetasks.validation.capabilitycheck;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.validation.capabilities.EmailSenderCapability;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;

@Slf4j
public class EmailSenderCapabilityCheck implements CapabilityCheck {
  @Inject private EmailHandler emailHandler;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    EmailSenderCapability emailSenderCapability = (EmailSenderCapability) delegateCapability;
    EmailRequest emailRequest = emailSenderCapability.getEmailRequest();
    return CapabilityResponse.builder()
        .delegateCapability(emailSenderCapability)
        .validated(emailHandler.validateDelegateConnection(emailRequest))
        .build();
  }
}
