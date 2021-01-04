package software.wings.delegatetasks.validation.capabilitycheck;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;

import software.wings.delegatetasks.validation.capabilities.SmtpCapability;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
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
