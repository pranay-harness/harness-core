package io.harness.delegate.beans.ci.pod;

import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import com.esotericsoftware.kryo.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SSHKeyDetails {
  @NotNull SSHKeyReferenceCredentialDTO sshKeyReference;
  @NotNull List<EncryptedDataDetail> encryptedDataDetails;
}
