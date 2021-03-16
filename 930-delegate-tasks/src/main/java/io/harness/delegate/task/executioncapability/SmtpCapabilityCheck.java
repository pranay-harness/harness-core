package io.harness.delegate.task.executioncapability;

import static java.time.Duration.ofMillis;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.SmtpParameters;
import io.harness.concurrent.HTimeLimiter;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SmtpCapability;

import java.util.Properties;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmtpCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  private static final HTimeLimiter timeLimiter = new HTimeLimiter();

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SmtpCapability capability = (SmtpCapability) delegateCapability;
    boolean capable = isCapable(capability.isUseSSL(), capability.isStartTLS(), capability.getHost(),
        capability.getPort(), capability.getUsername());
    return CapabilityResponse.builder().delegateCapability(capability).validated(capable).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SMTP_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    SmtpParameters params = parameters.getSmtpParameters();
    return builder
        .permissionResult(isCapable(params.getUseSsl(), params.getStartTls(), params.getHost(), params.getPort(),
                              params.getUsername())
                ? PermissionResult.ALLOWED
                : PermissionResult.DENIED)
        .build();
  }

  static boolean isCapable(boolean useSsl, boolean startTls, String host, int port, String username) {
    try {
      return timeLimiter.callInterruptible(ofMillis(10000), () -> {
        boolean result = false;
        try {
          Properties props = new Properties();
          if (useSsl) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.starttls.enable", "true");
          }
          if (startTls) {
            props.put("mail.smtp.starttls.enable", "true");
          }
          Session session = Session.getInstance(props, null);
          Transport transport = session.getTransport("smtp");
          transport.connect(host, port, username, "" /* no password check */);
          transport.close();
          result = true;
          log.info("Validated email delegate communication to {}.", transport.toString());
        } catch (AuthenticationFailedException e) {
          // if there is an authentication exception, we know that we have contacted the server, and thus the
          // capability is considered true.
          result = true;
        } catch (MessagingException e) {
          log.warn("SMTP: Messaging Exception Occurred", e);
        } catch (Exception e) {
          log.warn("SMTP: Unknown Exception", e);
        }
        return result;
      });
    } catch (Exception e) {
      log.warn("Failed to validate email delegate communication", e);
    }
    return false;
  }
}
