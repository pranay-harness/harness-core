/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
