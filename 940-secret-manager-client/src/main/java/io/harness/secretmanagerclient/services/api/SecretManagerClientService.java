package io.harness.secretmanagerclient.services.api;

import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.NGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;

import java.util.List;

public interface SecretManagerClientService {
  @Deprecated List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting encryptableSetting);

  List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity consumer);
}
