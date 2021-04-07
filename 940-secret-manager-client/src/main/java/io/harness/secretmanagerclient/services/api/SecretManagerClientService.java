package io.harness.secretmanagerclient.services.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.NGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

@OwnedBy(PL)
public interface SecretManagerClientService {
  List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity consumer);
}
