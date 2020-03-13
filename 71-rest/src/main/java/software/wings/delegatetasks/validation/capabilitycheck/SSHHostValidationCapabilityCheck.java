package software.wings.delegatetasks.validation.capabilitycheck;

import static java.time.Duration.ofSeconds;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtils.populateBuilderWithCredentials;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.CapabilityResponse.CapabilityResponseBuilder;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.delegatetasks.validation.capabilities.SSHHostValidationCapability;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

@Slf4j
public class SSHHostValidationCapabilityCheck implements CapabilityCheck {
  @Inject private EncryptionService encryptionService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SSHHostValidationCapability capability = (SSHHostValidationCapability) delegateCapability;
    CapabilityResponseBuilder capabilityResponseBuilder = CapabilityResponse.builder().delegateCapability(capability);

    if (capability.getValidationInfo().isExecuteOnDelegate()) {
      return capabilityResponseBuilder.validated(true).build();
    }

    decryptCredentials(capability.getHostConnectionAttributes(), capability.getBastionConnectionAttributes(),
        capability.getHostConnectionCredentials(), capability.getBastionConnectionCredentials());
    try {
      SshSessionConfig hostConnectionTest = createSshSessionConfig(capability);
      int timeout = (int) ofSeconds(15L).toMillis();
      hostConnectionTest.setSocketConnectTimeout(timeout);
      hostConnectionTest.setSshConnectionTimeout(timeout);
      hostConnectionTest.setSshSessionTimeout(timeout);
      getSSHSession(hostConnectionTest).disconnect();
      capabilityResponseBuilder.validated(true);
    } catch (Exception e) {
      logger.error("Failed to validate host - public dns:" + capability.getValidationInfo().getPublicDns(), e);
      capabilityResponseBuilder.validated(false);
    }
    return capabilityResponseBuilder.build();
  }

  private void decryptCredentials(SettingAttribute hostConnectionAttributes,
      SettingAttribute bastionConnectionAttributes, List<EncryptedDataDetail> hostConnectionCredential,
      List<EncryptedDataDetail> bastionConnectionCredential) {
    if (hostConnectionAttributes != null) {
      encryptionService.decrypt(
          (HostConnectionAttributes) hostConnectionAttributes.getValue(), hostConnectionCredential);
    }
    if (bastionConnectionAttributes != null) {
      encryptionService.decrypt(
          (BastionConnectionAttributes) bastionConnectionAttributes.getValue(), bastionConnectionCredential);
    }
  }

  public static SshSessionConfig createSshSessionConfig(SSHHostValidationCapability capability) {
    SshSessionConfig.Builder builder = aSshSessionConfig()
                                           .withAccountId(capability.getValidationInfo().getAccountId())
                                           .withAppId(capability.getValidationInfo().getAppId())
                                           .withExecutionId(capability.getValidationInfo().getAccountId())
                                           .withHost(capability.getValidationInfo().getPublicDns())
                                           .withCommandUnitName("HOST_CONNECTION_TEST");

    // TODO: The following can be removed as we do not support username and password from context anymore
    SSHExecutionCredential sshExecutionCredential = capability.getSshExecutionCredential();
    if (sshExecutionCredential != null) {
      builder.withUserName(sshExecutionCredential.getSshUser())
          .withPassword(sshExecutionCredential.getSshPassword())
          .withSudoAppName(sshExecutionCredential.getAppAccount())
          .withSudoAppPassword(sshExecutionCredential.getAppAccountPassword());
    }

    populateBuilderWithCredentials(
        builder, capability.getHostConnectionAttributes(), capability.getBastionConnectionAttributes());
    return builder.build();
  }
}
