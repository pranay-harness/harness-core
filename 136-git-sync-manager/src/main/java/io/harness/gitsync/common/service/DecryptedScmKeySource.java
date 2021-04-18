package io.harness.gitsync.common.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.connector.scm.ScmConnector;

@OwnedBy(HarnessTeam.DX)
public interface DecryptedScmKeySource {
  ScmConnector fetchKey(IdentifierRef scmConnectorRef);

  void removeKey(IdentifierRef scmConnectorRef);
}
