package io.harness.delegate.task.shell;

import static io.harness.shell.AuthenticationScheme.KERBEROS;
import static io.harness.shell.AuthenticationScheme.SSH_KEY;
import static io.harness.shell.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.AccessType;
import io.harness.shell.KerberosConfig;
import io.harness.shell.KerberosConfig.KerberosConfigBuilder;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class SshSessionConfigMapper {
  @Inject private SecretDecryptionService secretDecryptionService;

  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    SshSessionConfig.Builder builder = aSshSessionConfig().withPort(sshKeySpecDTO.getPort());
    SSHAuthDTO authDTO = sshKeySpecDTO.getAuth();
    switch (authDTO.getAuthScheme()) {
      case SSH:
        SSHConfigDTO sshConfigDTO = (SSHConfigDTO) authDTO.getSpec();
        generateSSHBuilder(sshConfigDTO, builder, encryptionDetails);
        break;
      case Kerberos:
        KerberosConfigDTO kerberosConfigDTO = (KerberosConfigDTO) authDTO.getSpec();
        generateKerberosBuilder(kerberosConfigDTO, builder, encryptionDetails);
        break;
      default:
        break;
    }
    return builder.build();
  }

  private void generateSSHBuilder(
      SSHConfigDTO sshConfigDTO, SshSessionConfig.Builder builder, List<EncryptedDataDetail> encryptionDetails) {
    switch (sshConfigDTO.getCredentialType()) {
      case Password:
        SSHPasswordCredentialDTO sshPasswordCredentialDTO = (SSHPasswordCredentialDTO) sshConfigDTO.getSpec();
        SSHPasswordCredentialDTO passwordCredentialDTO =
            (SSHPasswordCredentialDTO) secretDecryptionService.decrypt(sshPasswordCredentialDTO, encryptionDetails);
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
                sshKeyReferenceCredentialDTO, encryptionDetails);
        char[] fileData = keyReferenceCredentialDTO.getKey().getDecryptedValue();
        keyReferenceCredentialDTO.getKey().setDecryptedValue(new String(fileData).toCharArray());
        builder.withAccessType(AccessType.KEY)
            .withKeyName("Key")
            .withKey(keyReferenceCredentialDTO.getKey().getDecryptedValue())
            .withUserName(keyReferenceCredentialDTO.getUserName());
        break;
      case KeyPath:
        SSHKeyPathCredentialDTO sshKeyPathCredentialDTO = (SSHKeyPathCredentialDTO) sshConfigDTO.getSpec();
        SSHKeyPathCredentialDTO keyPathCredentialDTO =
            (SSHKeyPathCredentialDTO) secretDecryptionService.decrypt(sshKeyPathCredentialDTO, encryptionDetails);
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

  private void generateKerberosBuilder(KerberosConfigDTO kerberosConfigDTO, SshSessionConfig.Builder builder,
      List<EncryptedDataDetail> encryptionDetails) {
    KerberosConfigBuilder kerberosConfigBuilder = KerberosConfig.builder()
                                                      .principal(kerberosConfigDTO.getPrincipal())
                                                      .realm(kerberosConfigDTO.getRealm())
                                                      .generateTGT(kerberosConfigDTO.getTgtGenerationMethod() != null);
    switch (kerberosConfigDTO.getTgtGenerationMethod()) {
      case Password:
        TGTPasswordSpecDTO tgtPasswordSpecDTO = (TGTPasswordSpecDTO) kerberosConfigDTO.getSpec();
        TGTPasswordSpecDTO passwordSpecDTO =
            (TGTPasswordSpecDTO) secretDecryptionService.decrypt(tgtPasswordSpecDTO, encryptionDetails);
        builder.withPassword(passwordSpecDTO.getPassword().getDecryptedValue());
        break;
      case KeyTabFilePath:
        TGTKeyTabFilePathSpecDTO tgtKeyTabFilePathSpecDTO = (TGTKeyTabFilePathSpecDTO) kerberosConfigDTO.getSpec();
        TGTKeyTabFilePathSpecDTO keyTabFilePathSpecDTO =
            (TGTKeyTabFilePathSpecDTO) secretDecryptionService.decrypt(tgtKeyTabFilePathSpecDTO, encryptionDetails);
        kerberosConfigBuilder.keyTabFilePath(keyTabFilePathSpecDTO.getKeyPath());
        break;
      default:
        break;
    }
    builder.withAuthenticationScheme(KERBEROS)
        .withAccessType(AccessType.KERBEROS)
        .withKerberosConfig(kerberosConfigBuilder.build());
  }
}
