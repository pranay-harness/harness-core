package io.harness.cdng.secrets.tasks;

import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.SSH_KEY;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.SSHTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.KerberosConfig;
import software.wings.beans.KerberosConfig.KerberosConfigBuilder;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionFactory;

import com.google.inject.Inject;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class SSHConfigValidationDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService secretDecryptionService;

  public SSHConfigValidationDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  private SshSessionConfig getSSHSessionConfig(SSHTaskParams sshTaskParams) {
    SSHKeySpecDTO sshKeySpecDTO = sshTaskParams.getSshKeySpec();
    SshSessionConfig.Builder builder = aSshSessionConfig().withPort(sshKeySpecDTO.getPort());
    SSHAuthDTO authDTO = sshKeySpecDTO.getAuth();
    switch (authDTO.getAuthScheme()) {
      case SSH:
        SSHConfigDTO sshConfigDTO = (SSHConfigDTO) authDTO.getSpec();
        generateSSHBuilder(sshTaskParams, sshConfigDTO, builder);
        break;
      case Kerberos:
        KerberosConfigDTO kerberosConfigDTO = (KerberosConfigDTO) authDTO.getSpec();
        generateKerberosBuilder(sshTaskParams, kerberosConfigDTO, builder);
        break;
      default:
        break;
    }
    return builder.build();
  }

  private void generateSSHBuilder(
      SSHTaskParams sshTaskParams, SSHConfigDTO sshConfigDTO, SshSessionConfig.Builder builder) {
    switch (sshConfigDTO.getCredentialType()) {
      case Password:
        SSHPasswordCredentialDTO sshPasswordCredentialDTO = (SSHPasswordCredentialDTO) sshConfigDTO.getSpec();
        SSHPasswordCredentialDTO passwordCredentialDTO = (SSHPasswordCredentialDTO) secretDecryptionService.decrypt(
            sshPasswordCredentialDTO, sshTaskParams.getEncryptionDetails());
        builder.withAccessType(AccessType.USER_PASSWORD)
            .withUserName(passwordCredentialDTO.getUserName())
            .withPassword(passwordCredentialDTO.getPassword().getDecryptedValue());
        break;
      case KeyReference:
        SSHKeyReferenceCredentialDTO sshKeyReferenceCredentialDTO =
            (SSHKeyReferenceCredentialDTO) sshConfigDTO.getSpec();
        // since files are base 64 encoded, we decode it before using it
        SSHKeyReferenceCredentialDTO keyReferenceCredentialDTO =
            (SSHKeyReferenceCredentialDTO) secretDecryptionService.decrypt(
                sshKeyReferenceCredentialDTO, sshTaskParams.getEncryptionDetails());
        char[] fileData = keyReferenceCredentialDTO.getKey().getDecryptedValue();
        keyReferenceCredentialDTO.getKey().setDecryptedValue(new String(fileData).toCharArray());
        builder.withAccessType(AccessType.KEY)
            .withKeyName("Key")
            .withKey(keyReferenceCredentialDTO.getKey().getDecryptedValue())
            .withUserName(keyReferenceCredentialDTO.getUserName());
        break;
      case KeyPath:
        SSHKeyPathCredentialDTO sshKeyPathCredentialDTO = (SSHKeyPathCredentialDTO) sshConfigDTO.getSpec();
        SSHKeyPathCredentialDTO keyPathCredentialDTO = (SSHKeyPathCredentialDTO) secretDecryptionService.decrypt(
            sshKeyPathCredentialDTO, sshTaskParams.getEncryptionDetails());
        builder.withKeyPath(keyPathCredentialDTO.getKeyPath())
            .withUserName(keyPathCredentialDTO.getUserName())
            .withAccessType(AccessType.KEY)
            .withKeyLess(true)
            .build();
        break;
      default:
        break;
    }
    builder.withAuthenticationScheme(SSH_KEY);
  }

  private void generateKerberosBuilder(
      SSHTaskParams sshTaskParams, KerberosConfigDTO kerberosConfigDTO, SshSessionConfig.Builder builder) {
    KerberosConfigBuilder kerberosConfig = KerberosConfig.builder()
                                               .principal(kerberosConfigDTO.getPrincipal())
                                               .realm(kerberosConfigDTO.getRealm())
                                               .generateTGT(kerberosConfigDTO.getTgtGenerationMethod() != null);
    switch (kerberosConfigDTO.getTgtGenerationMethod()) {
      case Password:
        TGTPasswordSpecDTO tgtPasswordSpecDTO = (TGTPasswordSpecDTO) kerberosConfigDTO.getSpec();
        TGTPasswordSpecDTO passwordSpecDTO = (TGTPasswordSpecDTO) secretDecryptionService.decrypt(
            tgtPasswordSpecDTO, sshTaskParams.getEncryptionDetails());
        builder.withPassword(passwordSpecDTO.getPassword().getDecryptedValue());
        break;
      case KeyTabFilePath:
        TGTKeyTabFilePathSpecDTO tgtKeyTabFilePathSpecDTO = (TGTKeyTabFilePathSpecDTO) kerberosConfigDTO.getSpec();
        TGTKeyTabFilePathSpecDTO keyTabFilePathSpecDTO = (TGTKeyTabFilePathSpecDTO) secretDecryptionService.decrypt(
            tgtKeyTabFilePathSpecDTO, sshTaskParams.getEncryptionDetails());
        kerberosConfig.keyTabFilePath(keyTabFilePathSpecDTO.getKeyPath());
        break;
      default:
        break;
    }
    builder.withAuthenticationScheme(KERBEROS)
        .withAccessType(AccessType.KERBEROS)
        .withKerberosConfig(kerberosConfig.build());
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    SSHTaskParams sshTaskParams = (SSHTaskParams) parameters;

    SshSessionConfig sshSessionConfig = getSSHSessionConfig(sshTaskParams);
    sshSessionConfig.setHost(sshTaskParams.getHost());
    try {
      Session session = SshSessionFactory.getSSHSession(sshSessionConfig);
      session.disconnect();
      return SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build();
    } catch (JSchException e) {
      return SSHConfigValidationTaskResponse.builder().connectionSuccessful(false).errorMessage(e.getMessage()).build();
    }
  }
}
