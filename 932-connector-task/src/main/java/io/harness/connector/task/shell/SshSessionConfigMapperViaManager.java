package io.harness.connector.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.shell.AuthenticationScheme.KERBEROS;
import static io.harness.shell.AuthenticationScheme.SSH_KEY;
import static io.harness.shell.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.ng.core.dto.secrets.*;
import io.harness.ng.core.remote.NGSecretDecryptionClient;
import io.harness.remote.client.NGRestClientExecutor;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.AccessType;
import io.harness.shell.KerberosConfig;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDP)
@Singleton
public class SshSessionConfigMapperViaManager {
  @Inject NGSecretDecryptionClient ngSecretDecryptionClient;
  @Inject NGRestClientExecutor restClientExecutor;

  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    SshSessionConfig.Builder builder = aSshSessionConfig().withPort(sshKeySpecDTO.getPort());
    SSHAuthDTO authDTO = sshKeySpecDTO.getAuth();
    switch (authDTO.getAuthScheme()) {
      case SSH:
        SSHConfigDTO sshConfigDTO = (SSHConfigDTO) authDTO.getSpec();
        final SSHCredentialSpecDTO spec = sshConfigDTO.getSpec();
        final DecryptableEntityWithEncryptionConsumers build = DecryptableEntityWithEncryptionConsumers.builder()
                                                                   .decryptableEntity((DecryptableEntity) spec)
                                                                   .encryptedDataDetailList(encryptionDetails)
                                                                   .build();
        final DecryptableEntity response = restClientExecutor.getResponse(
            ngSecretDecryptionClient.decryptEncryptedDetails(build, "kmpySmUISimoRrJL6NL73w"));
        generateSSHBuilder(sshConfigDTO, builder, encryptionDetails, response);
        break;
      case Kerberos:
        KerberosConfigDTO kerberosConfigDTO = (KerberosConfigDTO) authDTO.getSpec();
        final DecryptableEntityWithEncryptionConsumers decryptableEntityWithEncryptionConsumers =
            DecryptableEntityWithEncryptionConsumers.builder()
                .decryptableEntity((DecryptableEntity) kerberosConfigDTO)
                .encryptedDataDetailList(encryptionDetails)
                .build();
        final DecryptableEntity decryptableEntity =
            restClientExecutor.getResponse(ngSecretDecryptionClient.decryptEncryptedDetails(
                decryptableEntityWithEncryptionConsumers, "kmpySmUISimoRrJL6NL73w"));

        generateKerberosBuilder(kerberosConfigDTO, builder, encryptionDetails, decryptableEntity);
        break;
      default:
        break;
    }
    return builder.build();
  }

  private void generateSSHBuilder(SSHConfigDTO sshConfigDTO, SshSessionConfig.Builder builder,
      List<EncryptedDataDetail> encryptionDetails, DecryptableEntity decryptableEntity) {
    switch (sshConfigDTO.getCredentialType()) {
      case Password:
        SSHPasswordCredentialDTO sshPasswordCredentialDTO = (SSHPasswordCredentialDTO) decryptableEntity;

        builder.withAccessType(AccessType.USER_PASSWORD)
            .withUserName(sshPasswordCredentialDTO.getUserName())
            .withPassword(sshPasswordCredentialDTO.getPassword().getDecryptedValue());
        break;
      case KeyReference:
        SSHKeyReferenceCredentialDTO sshKeyReferenceCredentialDTO = (SSHKeyReferenceCredentialDTO) decryptableEntity;

        char[] fileData = sshKeyReferenceCredentialDTO.getKey().getDecryptedValue();
        sshKeyReferenceCredentialDTO.getKey().setDecryptedValue(new String(fileData).toCharArray());
        builder.withAccessType(AccessType.KEY)
            .withKeyName("Key")
            .withKey(sshKeyReferenceCredentialDTO.getKey().getDecryptedValue())
            .withUserName(sshKeyReferenceCredentialDTO.getUserName());
        if (null != sshKeyReferenceCredentialDTO.getEncryptedPassphrase()) {
          builder.withKeyPassphrase(sshKeyReferenceCredentialDTO.getEncryptedPassphrase().getDecryptedValue());
        }
        break;
      case KeyPath:
        SSHKeyPathCredentialDTO sshKeyPathCredentialDTO = (SSHKeyPathCredentialDTO) decryptableEntity;

        builder.withKeyPath(sshKeyPathCredentialDTO.getKeyPath())
            .withUserName(sshKeyPathCredentialDTO.getUserName())
            .withAccessType(AccessType.KEY)
            .withKeyLess(true)
            .build();
        break;
      default:
        break;
    }
    builder.withAuthenticationScheme(SSH_KEY);
  }

  private void generateKerberosBuilder(KerberosConfigDTO kerberosConfigDTO, SshSessionConfig.Builder builder,
      List<EncryptedDataDetail> encryptionDetails, DecryptableEntity decryptableEntity) {
    KerberosConfig.KerberosConfigBuilder kerberosConfigBuilder =
        KerberosConfig.builder()
            .principal(kerberosConfigDTO.getPrincipal())
            .realm(kerberosConfigDTO.getRealm())
            .generateTGT(kerberosConfigDTO.getTgtGenerationMethod() != null);
    switch (kerberosConfigDTO.getTgtGenerationMethod()) {
      case Password:
        TGTPasswordSpecDTO tgtPasswordSpecDTO = (TGTPasswordSpecDTO) decryptableEntity;

        builder.withPassword(tgtPasswordSpecDTO.getPassword().getDecryptedValue());
        break;
      case KeyTabFilePath:
        TGTKeyTabFilePathSpecDTO tgtKeyTabFilePathSpecDTO = (TGTKeyTabFilePathSpecDTO) decryptableEntity;

        kerberosConfigBuilder.keyTabFilePath(tgtKeyTabFilePathSpecDTO.getKeyPath());
        break;
      default:
        break;
    }
    builder.withAuthenticationScheme(KERBEROS)
        .withAccessType(AccessType.KERBEROS)
        .withKerberosConfig(kerberosConfigBuilder.build());
  }
}
