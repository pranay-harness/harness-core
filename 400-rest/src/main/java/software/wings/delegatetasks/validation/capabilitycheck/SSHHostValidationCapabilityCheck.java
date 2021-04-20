package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.shell.SshSessionConfig.Builder.aSshSessionConfig;
import static io.harness.shell.SshSessionFactory.getSSHSession;

import static software.wings.utils.SshHelperUtils.populateBuilderWithCredentials;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FeatureName;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.CapabilityResponse.CapabilityResponseBuilder;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.ff.FeatureFlagService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.SshSessionConfig;

import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.validation.capabilities.SSHHostValidationCapability;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class SSHHostValidationCapabilityCheck implements CapabilityCheck {
  @Inject private DelegateConfiguration delegateConfiguration;
  @Inject private EncryptionService encryptionService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private SecretManagementDelegateService secretManagementDelegateService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SSHHostValidationCapability capability = (SSHHostValidationCapability) delegateCapability;
    CapabilityResponseBuilder capabilityResponseBuilder = CapabilityResponse.builder().delegateCapability(capability);

    boolean rolledOut = featureFlagService.isEnabled(
        FeatureName.SSH_HOST_VALIDATION_STRIP_SECRETS, delegateConfiguration.getAccountId());
    boolean abTesting = featureFlagService.isEnabled(
        FeatureName.SSH_HOST_VALIDATION_STRIP_SECRETS_AB, delegateConfiguration.getAccountId());

    boolean capabilityValidatedWithSecrets = false;
    boolean capabilityValidatedWithoutSecrets = false;

    if (!rolledOut) {
      decryptCredentials(capability.getHostConnectionAttributes(), capability.getBastionConnectionAttributes(),
          capability.getHostConnectionCredentials(), capability.getBastionConnectionCredentials(),
          capability.getSshVaultConfig());
      try {
        SshSessionConfig hostConnectionTest = createSshSessionConfig(capability);
        int timeout = (int) ofSeconds(15L).toMillis();
        hostConnectionTest.setSocketConnectTimeout(timeout);
        hostConnectionTest.setSshConnectionTimeout(timeout);
        hostConnectionTest.setSshSessionTimeout(timeout);
        performTest(hostConnectionTest);
        capabilityValidatedWithSecrets = true;
      } catch (Exception e) {
        log.error("Failed to validate host - public dns:" + capability.getValidationInfo().getPublicDns(), e);
        capabilityValidatedWithSecrets = false;
      }
    }
    if (rolledOut || abTesting) {
      try {
        capabilityValidatedWithoutSecrets =
            checkConnectivity(capability.getHost(), capability.getPort(), abTesting, capabilityValidatedWithSecrets);
      } catch (Exception e) {
        log.warn("SSH_HOST_VALIDATION_STRIP_SECRETS: something went totally wrong: " + e);
      }
    }

    if (abTesting) {
      if (capabilityValidatedWithoutSecrets ^ capabilityValidatedWithSecrets) {
        log.warn("SSH_HOST_VALIDATION_STRIP_SECRETS (was " + capabilityValidatedWithSecrets
            + ") encountered differences: " + capability);
      }
    }

    return capabilityResponseBuilder
        .validated(rolledOut ? capabilityValidatedWithoutSecrets : capabilityValidatedWithSecrets)
        .build();
  }

  @VisibleForTesting
  void performTest(SshSessionConfig hostConnectionTest) throws JSchException {
    getSSHSession(hostConnectionTest).disconnect();
  }

  private void decryptCredentials(SettingAttribute hostConnectionAttributes,
      SettingAttribute bastionConnectionAttributes, List<EncryptedDataDetail> hostConnectionCredential,
      List<EncryptedDataDetail> bastionConnectionCredential, SSHVaultConfig sshVaultConfig) {
    if (hostConnectionAttributes != null) {
      encryptionService.decrypt(
          (HostConnectionAttributes) hostConnectionAttributes.getValue(), hostConnectionCredential, false);
      if (hostConnectionAttributes.getValue() instanceof HostConnectionAttributes
          && ((HostConnectionAttributes) hostConnectionAttributes.getValue()).isVaultSSH()) {
        secretManagementDelegateService.signPublicKey(
            (HostConnectionAttributes) hostConnectionAttributes.getValue(), sshVaultConfig);
      }
    }
    if (bastionConnectionAttributes != null) {
      encryptionService.decrypt(
          (BastionConnectionAttributes) bastionConnectionAttributes.getValue(), bastionConnectionCredential, false);
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

  boolean checkConnectivity(String host, int port, boolean abTesting, boolean otherResult) {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession("username_placeholder", host, port);
      session.connect((int) ofSeconds(15L).toMillis());
      session.disconnect();
      return true;
    } catch (JSchException jschException) {
      if (jschException.getMessage().contains("Auth fail")) {
        // if we had the correct password, it would have connected, so we mark it as true.
        return true;
      } else {
        if (abTesting) {
          log.info("SSH_HOST_VALIDATION_STRIP_SECRETS baseline " + otherResult + " and encountered " + jschException);
        }
        return false;
      }
    } catch (Exception e) {
      if (abTesting) {
        log.warn("SSHHostValidationCapabilityCheck: Unexpected exception : " + e);
      }
      return false;
    }
  }
}
